import com.fasterxml.jackson.databind.ObjectMapper
import com.github.chekist32.jooq.goipay.tables.references.CRYPTO_ADDRESSES
import com.github.chekist32.jooq.goipay.tables.references.CRYPTO_DATA
import com.github.chekist32.jooq.goipay.tables.references.INVOICES
import com.github.chekist32.jooq.goipay.tables.references.XMR_CRYPTO_DATA
import com.github.chekist32.jooq.sd.enums.CryptoType
import com.github.chekist32.jooq.sd.tables.references.DONATIONS
import com.github.chekist32.jooq.sd.tables.references.DONATION_PROFILE_DATA
import com.github.chekist32.jooq.sd.tables.references.USERS
import com.github.chekist32.payment.PaymentGrpcNotificationService
import com.github.chekist32.user.CryptoKeysData
import com.github.chekist32.user.XmrKeys
import crypto.v1.Crypto
import dasniko.testcontainers.keycloak.KeycloakContainer
import invoice.v1.InvoiceServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.AbstractStub
import io.minio.*
import io.minio.messages.DeleteObject
import io.quarkus.grpc.GrpcClient
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import jakarta.annotation.PostConstruct
import jakarta.inject.Inject
import jakarta.inject.Named
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.keycloak.OAuth2Constants
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.RealmRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.testcontainers.containers.*
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import user.v1.User
import user.v1.UserServiceGrpc
import java.nio.file.Files
import java.nio.file.Paths
import java.security.SecureRandom
import java.time.Duration
import java.util.*


fun getEnvOrThrow(name: String): String {
    val value = System.getenv(name)
    if (value.isEmpty()) throw IllegalArgumentException("Env var $name must be specified")
    return value
}

@QuarkusTestResource(MinioTestResource::class)
@QuarkusTestResource(KeycloakTestResource::class)
@QuarkusTestResource(GoipayTestResource::class)
@QuarkusTestResource(SdTestResource::class)
abstract class BasicIntegrationTest {
    protected val rand = SecureRandom()

    protected lateinit var keycloakAdminClient: Keycloak
    private lateinit var minioClient: MinioClient

    @field:Inject
    @field:Named("dsl-sd")
    protected lateinit var dslSD: DSLContext
    @field:Inject
    @field:Named("dsl-goipay")
    protected lateinit var dslGoipay: DSLContext

    @field:Inject
    @field:GrpcClient("goipay")
    protected lateinit var userGoipayGrpcClient: UserServiceGrpc.UserServiceBlockingStub
    @field:Inject
    @field:GrpcClient("goipay")
    protected lateinit var paymentGoipayGrpcClient: InvoiceServiceGrpc.InvoiceServiceBlockingStub
    protected lateinit var paymentGrpcNotificationService: PaymentGrpcNotificationService

    private lateinit var goipayContainer: GenericContainer<*>
    private lateinit var grpcChannel: ManagedChannel

    companion object {
        private val objectMapper = ObjectMapper()

        private const val bucketName = "salmon-donate"

        private val avatarBytes = Files.readAllBytes(Paths.get("src/test/resources/avatar.jpeg"))
        val expectedAvatar get() = avatarBytes.copyOf()

        private val realmConfig = Files.readString(Paths.get("keycloak/realms/realm-export.json"))
        private val baseKeycloakUrl = getEnvOrThrow("SD_KEYCLOAK_BASE_URL")
        val realmName = getEnvOrThrow("SD_KEYCLOAK_REALM")

        val testXmrKeys = XmrKeys(
            priv = "8aa763d1c8d9da4ca75cb6ca22a021b5cca376c1367be8d62bcc9cdf4b926009",
            pub = "38e9908d33d034de0ba1281aa7afe3907b795cea14852b3d8fe276e8931cb130"
        )
    }

    @PostConstruct
    protected fun post() {
        keycloakAdminClient = KeycloakBuilder.builder()
            .serverUrl(baseKeycloakUrl)
            .clientId("admin-cli")
            .username("admin")
            .password("admin")
            .realm("master")
            .grantType(OAuth2Constants.PASSWORD)
            .build()

        minioClient = MinioClient.builder()
            .endpoint("${getEnvOrThrow("SD_MINIO_URL")}:${getEnvOrThrow("SD_MINIO_PORT")}")
            .credentials(getEnvOrThrow("SD_MINIO_ACCESS_KEY"), getEnvOrThrow("SD_MINIO_SECRET_KEY"))
            .build()
    }

    private fun setupKeycloak() {
        val userId = registerKeycloakUser(
            TestUser.TestUserBuilder().also {
                it.username = getEnvOrThrow("SD_KEYCLOAK_REALM_ADMIN_USERNAME")
                it.password = getEnvOrThrow("SD_KEYCLOAK_REALM_ADMIN_PASSWORD")
            }.build()
        )

        val clientId = keycloakAdminClient.realm(realmName)
            .clients()
            .findAll()
            .find { it.clientId == "realm-management" }?.id ?: throw IllegalStateException("Failed to fetch client")

        val roles = keycloakAdminClient.realm(realmName)
            .clients()
            .get(clientId)
            .roles()
            .list()
            .filter { arrayOf("manage-users", "query-users").contains(it.name) }

        keycloakAdminClient.realm(realmName)
            .users()
            .get(userId.toString())
            .roles()
            .clientLevel(clientId)
            .add(roles)
    }

    private fun setupMinio() {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build())
        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
            .bucket(bucketName)
            .config("""
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Principal": {
                                "AWS": [
                                    "*"
                                ]
                            },
                            "Action": [
                                "s3:GetObject"
                            ],
                            "Resource": [
                                "arn:aws:s3:::$bucketName/public/*"
                            ]
                        }
                    ]
                }
            """.trimIndent())
            .build()
        )
    }

    private fun setupGrpcGoipay() {
        val channelField = AbstractStub::class.java.getDeclaredField("channel")
        channelField.isAccessible = true
        channelField.set(userGoipayGrpcClient, grpcChannel)
        channelField.set(paymentGoipayGrpcClient, grpcChannel)
    }

    private fun setupGrpcNotificationService() {
        paymentGrpcNotificationService = PaymentGrpcNotificationService(paymentGoipayGrpcClient)
    }

    private fun setupGrpc() {
        setupGrpcGoipay()
        setupGrpcNotificationService()
    }

    @BeforeEach
    protected fun setup() {
        setupGrpc()
        setupKeycloak()
        setupMinio()
    }

    private fun resetMinio() {
        minioClient.removeObjects(RemoveObjectsArgs.builder()
            .bucket(bucketName)
            .objects(minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).recursive(true).build()).map { DeleteObject(it.get().objectName()) })
            .build()
        ).forEach { it.get() }
        minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build())
    }

    private fun resetKeycloak() {
        keycloakAdminClient.realm(realmName).remove()
        keycloakAdminClient.realms().create(objectMapper.readValue(realmConfig, RealmRepresentation::class.java))
    }

    private fun resetGoipay() {
        dslGoipay.truncate(INVOICES).cascade().execute()
        dslGoipay.truncate(CRYPTO_ADDRESSES).cascade().execute()
        dslGoipay.truncate(XMR_CRYPTO_DATA).cascade().execute()
        dslGoipay.truncate(CRYPTO_DATA).cascade().execute()
        dslGoipay.truncate(USERS).cascade().execute()

        goipayContainer.stop()
        goipayContainer.start()
    }

    private fun resetSd() {
        dslSD.truncate(DONATION_PROFILE_DATA).cascade().execute()
        dslSD.truncate(DONATIONS).cascade().execute()
        dslSD.truncate(USERS).cascade().execute()
    }

    @AfterEach
    protected fun reset() {
        resetGoipay()
        resetSd()
        resetKeycloak()
        resetMinio()
    }

    fun registerKeycloakUser(testUser: TestUser): UUID {
        val res = keycloakAdminClient.realm(realmName).users().create(UserRepresentation().also { user ->
            user.username = testUser.username
            user.email = testUser.email
            user.firstName = testUser.firstName
            user.lastName = testUser.lastName
            user.isEnabled = true
            user.isEmailVerified = true
            user.credentials = listOf(CredentialRepresentation().also { creds ->
                creds.type = CredentialRepresentation.PASSWORD
                creds.value = testUser.password
                creds.isTemporary = false
            })
        })
        if (res.status != 201) {
            throw IllegalStateException("Failed to register user: ${res.status} - ${res.readEntity(String::class.java)}")
        }

        return UUID.fromString(
            keycloakAdminClient.realm(realmName).users().searchByUsername(testUser.username, true).firstOrNull()?.id
                ?: throw IllegalStateException("Failed to get user")
        )
    }

    fun registerUserSd(userId: UUID) {
        dslSD.insertInto(USERS)
            .set(USERS.ID, userId)
            .execute()

        dslSD.insertInto(DONATION_PROFILE_DATA)
            .set(DONATION_PROFILE_DATA.USER_ID, userId)
            .execute()
    }

    fun registerUserFull(user: TestUser): UUID {
        val userId = registerKeycloakUser(user)
        registerUserSd(userId)
        userGoipayGrpcClient.registerUser(User.RegisterUserRequest.newBuilder().setUserId(userId.toString()).build())

        if (user.cryptoKeysData != null) {
            userGoipayGrpcClient.updateCryptoKeys(
                User.UpdateCryptoKeysRequest.newBuilder()
                    .setUserId(userId.toString())
                    .setXmrReq(
                        Crypto.XmrKeysUpdateRequest.newBuilder()
                            .setPrivViewKey(user.cryptoKeysData.xmr.keys.priv)
                            .setPubSpendKey(user.cryptoKeysData.xmr.keys.pub)
                            .build()
                    )
                    .build()
            )
            if (user.cryptoKeysData.xmr.enabled) {
                dslSD.update(DONATION_PROFILE_DATA)
                    .set(DONATION_PROFILE_DATA.ENABLED_CRYPTO,
                        DSL.arrayAppend(DONATION_PROFILE_DATA.ENABLED_CRYPTO, CryptoType.XMR)
                    )
                    .where(DONATION_PROFILE_DATA.USER_ID.eq(userId))
                    .execute()
            }
        }

        return userId
    }
}

data class TestUser(
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val password: String,
    val cryptoKeysData: CryptoKeysData?
) {
    class TestUserBuilder {
        var username: String = UUID.randomUUID().toString()
        var email: String = "${UUID.randomUUID()}@test.com"
        var firstName: String = "${UUID.randomUUID()}-1"
        var lastName: String = "${UUID.randomUUID()}-2"
        var password: String = UUID.randomUUID().toString()
        var cryptoKeysData: CryptoKeysData? = null

        fun build(): TestUser {
            return TestUser(
                username = username,
                email = email,
                firstName = firstName,
                lastName = lastName,
                password = password,
                cryptoKeysData = cryptoKeysData
            )
        }
    }
}

class SdTestResource: QuarkusTestResourceLifecycleManager {
    private lateinit var sdDbContainer: PostgreSQLContainer<*>

    override fun start(): MutableMap<String, String> {
        val net = Network.newNetwork()

        val sdDbContainerNetworkAlias = "sd-db-test"
        sdDbContainer = PostgreSQLContainer("postgres:16-alpine")
        sdDbContainer.withNetwork(net)
            .withNetworkAliases(sdDbContainerNetworkAlias)
            .withUsername(getEnvOrThrow("SD_DB_USER"))
            .withPassword(getEnvOrThrow("SD_DB_PASS"))
            .withDatabaseName(getEnvOrThrow("SD_DB_NAME"))
            .waitingFor(Wait.forSuccessfulCommand("pg_isready -U ${getEnvOrThrow("SD_DB_USER")} -d ${getEnvOrThrow("SD_DB_NAME")}").withStartupTimeout(Duration.ofSeconds(20)))

        sdDbContainer.portBindings = mutableListOf("${getEnvOrThrow("SD_DB_PORT")}:5432")
        sdDbContainer.start()

        val migrations = GenericContainer("liquibase/liquibase")
        migrations.withNetwork(net)
            .dependsOn(sdDbContainer)
            .withFileSystemBind("src/main/resources/db", "/liquibase/changelog", BindMode.READ_ONLY)
            .withCommand("liquibase --driver=org.postgresql.Driver --url=jdbc:postgresql://$sdDbContainerNetworkAlias:5432/${sdDbContainer.databaseName} --changeLogFile=./changelog/migrations-master.yml --username=${sdDbContainer.username} --password=${sdDbContainer.password} update")
            .waitingFor(Wait.forLogMessage(".*Liquibase command 'update' was executed successfully.*", 1))
            .withStartupCheckStrategy(OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(30)))
        migrations.start()

        return mutableMapOf()
    }

    override fun stop() {
        sdDbContainer.stop()
    }
}

class GoipayTestResource: QuarkusTestResourceLifecycleManager {
    private lateinit var goipayDbContainer: PostgreSQLContainer<*>
    private lateinit var goipayContainer: GenericContainer<*>

    private lateinit var grpcChannel: ManagedChannel

    override fun start(): MutableMap<String, String> {
        val net = Network.newNetwork()

        val goipayDbContainerNetworkAlias = "goipay-db-test"
        goipayDbContainer = PostgreSQLContainer("postgres:16-alpine")
        goipayDbContainer.withNetwork(net)
            .withNetworkAliases(goipayDbContainerNetworkAlias)
            .withUsername(getEnvOrThrow("GOIPAY_DB_USER"))
            .withPassword(getEnvOrThrow("GOIPAY_DB_PASS"))
            .withDatabaseName(getEnvOrThrow("GOIPAY_DB_NAME"))
            .waitingFor(Wait.forSuccessfulCommand("pg_isready -U ${getEnvOrThrow("GOIPAY_DB_USER")} -d ${getEnvOrThrow("GOIPAY_DB_NAME")}").withStartupTimeout(Duration.ofSeconds(20)))

        goipayDbContainer.portBindings = mutableListOf("${getEnvOrThrow("GOIPAY_DB_PORT")}:5432")

        goipayDbContainer.start()

        val migrations = GenericContainer("chekist32/goose-docker")
        migrations.withNetwork(net)
            .dependsOn(goipayDbContainer)
            .withFileSystemBind("external/goipay/sql/migrations", "/migrations", BindMode.READ_ONLY)
            .withEnv(mapOf(
                "GOOSE_DRIVER" to "postgres",
                "GOOSE_DBSTRING" to "host=$goipayDbContainerNetworkAlias port=5432 user=${goipayDbContainer.username} password=${goipayDbContainer.password} dbname=${goipayDbContainer.databaseName}"
            ))
            .waitingFor(Wait.forLogMessage(".*version.*", 1))
            .withStartupCheckStrategy(OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(20)))

        migrations.start()

        goipayContainer = GenericContainer("chekist32/goipay:v0.4.0")
        goipayContainer.withNetwork(net)
            .withEnv(mapOf(
                "SERVER_HOST" to "0.0.0.0",
                "SERVER_PORT" to "3000",
                "SERVER_TLS_MODE" to "none",
                "DATABASE_HOST" to "goipay-db-test",
                "DATABASE_PORT" to "5432",
                "DATABASE_USER" to goipayDbContainer.username,
                "DATABASE_PASS" to goipayDbContainer.password,
                "DATABASE_NAME" to goipayDbContainer.databaseName,
                "XMR_DAEMON_URL" to "http://node.monerodevs.org:38089"
            ))
            .waitingFor(Wait.forListeningPorts().withStartupTimeout(Duration.ofSeconds(10)))

        goipayContainer.portBindings = mutableListOf("${getEnvOrThrow("GOIPAY_PORT")}:3000")
        goipayContainer.start()

        grpcChannel = ManagedChannelBuilder
            .forAddress(getEnvOrThrow("GOIPAY_HOST"), getEnvOrThrow("GOIPAY_PORT").toInt())
            .usePlaintext()
            .build()

        return mutableMapOf()
    }

    override fun inject(testInjector: QuarkusTestResourceLifecycleManager.TestInjector) {
        testInjector.injectIntoFields(grpcChannel) { it.type == ManagedChannel::class.java && it.name == "grpcChannel" }
        testInjector.injectIntoFields(goipayContainer) { it.type == GenericContainer::class.java && it.name == "goipayContainer" }
    }

    override fun stop() {
        goipayContainer.stop()
        goipayDbContainer.stop()
    }
}

class KeycloakTestResource: QuarkusTestResourceLifecycleManager {
    private lateinit var keycloakContainer: KeycloakContainer

    override fun start(): MutableMap<String, String> {
        keycloakContainer = KeycloakContainer("quay.io/keycloak/keycloak:26.0")
        keycloakContainer.withRealmImportFile("realm-export.json")
            .withAdminUsername("admin")
            .withAdminPassword("admin")

        keycloakContainer.portBindings = mutableListOf("${getEnvOrThrow("SD_KEYCLOAK_BASE_URL").split(':').last()}:8080")
        keycloakContainer.start()

        return mutableMapOf()
    }

    override fun stop() {
        keycloakContainer.stop()
    }
}

class MinioTestResource: QuarkusTestResourceLifecycleManager {
    private lateinit var minioContainer: MinIOContainer

    override fun start(): MutableMap<String, String> {
        minioContainer = MinIOContainer(DockerImageName.parse("quay.io/minio/minio:RELEASE.2024-10-02T17-50-41Z").asCompatibleSubstituteFor("minio/minio"))
            .withUserName(getEnvOrThrow("SD_MINIO_ACCESS_KEY"))
            .withPassword(getEnvOrThrow("SD_MINIO_SECRET_KEY"))

        minioContainer.portBindings = mutableListOf("${getEnvOrThrow("SD_MINIO_PORT")}:9000")
        minioContainer.start()

        return mutableMapOf()
    }

    override fun stop() {
        minioContainer.stop()
    }
}