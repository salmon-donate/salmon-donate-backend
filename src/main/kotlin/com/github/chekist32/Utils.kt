package com.github.chekist32

import com.github.chekist32.jooq.goipay.enums.CoinType
import com.github.chekist32.jooq.goipay.enums.InvoiceStatusType
import com.github.chekist32.jooq.sd.enums.CryptoType
import com.github.chekist32.user.CryptoKeys
import com.github.chekist32.user.CryptoKeysData
import com.github.chekist32.user.XmrKeys
import com.github.chekist32.user.XmrKeysData
import com.google.protobuf.Timestamp
import crypto.v1.Crypto
import invoice.v1.InvoiceOuterClass
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.InternalServerErrorException
import jakarta.ws.rs.container.ContainerRequestContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.Executors


private val virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()
val Dispatchers.VT
    get() = virtualThreadExecutor.asCoroutineDispatcher()

fun ContainerRequestContext.getBodyWithoutModifying(): ByteArray {
    val body = entityStream.readAllBytes()
    entityStream = ByteArrayInputStream(body)
    return body
}

fun Timestamp.toUTCLocalDateTime(): LocalDateTime {
    return LocalDateTime.ofEpochSecond(this.seconds, this.nanos, ZoneOffset.UTC)
}

fun parseUserIdOrThrowBadRequest(userId: String): UUID {
    return parseUUIDOrThrowBadRequest(userId, "Invalid userId")
}
fun parseUUIDOrThrowBadRequest(id: String, msg: String): UUID {
    try { return UUID.fromString(id) }
    catch (e: IllegalArgumentException) { throw BadRequestException(msg) }
    catch (e: Exception) { throw InternalServerErrorException() }
}

fun CryptoKeys.toCryptoKeysData(enabledCrypto: Set<CryptoType>): CryptoKeysData {
    return CryptoKeysData(
        xmr = XmrKeysData(
            keys = XmrKeys(
                priv = xmr.priv,
                pub = xmr.pub
            ),
            enabled = enabledCrypto.contains(CryptoType.XMR)
        )
    )
}

fun CryptoKeys.toCoinTypes(enabledCrypto: Set<CryptoType>): Set<CryptoType> {
    val coinTypes = EnumSet.noneOf(CryptoType::class.java)
    if (enabledCrypto.contains(CryptoType.XMR) && xmr.priv.isNotBlank() && xmr.pub.isNotBlank()) {
        coinTypes.add(CryptoType.XMR)
    }

    return coinTypes
}

fun InvoiceOuterClass.InvoiceStatusType.toInvoiceStatusTypeJooq(): InvoiceStatusType {
    return when (this) {
        InvoiceOuterClass.InvoiceStatusType.PENDING -> InvoiceStatusType.PENDING
        InvoiceOuterClass.InvoiceStatusType.PENDING_MEMPOOL -> InvoiceStatusType.PENDING_MEMPOOL
        InvoiceOuterClass.InvoiceStatusType.EXPIRED -> InvoiceStatusType.EXPIRED
        InvoiceOuterClass.InvoiceStatusType.CONFIRMED -> InvoiceStatusType.CONFIRMED
        else -> throw IllegalArgumentException("invoice.v1.InvoiceOuterClass.InvoiceStatusType $this")
    }
}

fun Crypto.CoinType.toCoinTypeJooq(): CoinType {
    return when (this) {
        Crypto.CoinType.XMR -> CoinType.XMR
        Crypto.CoinType.BTC -> CoinType.BTC
        Crypto.CoinType.LTC -> CoinType.LTC
        Crypto.CoinType.ETH -> CoinType.ETH
        Crypto.CoinType.TON -> CoinType.TON
        else -> throw IllegalArgumentException("Invalid crypto.v1.Crypto.CoinType $this")
    }
}

fun CryptoType.toCoinType(): Crypto.CoinType {
    return when (this) {
        CryptoType.XMR -> Crypto.CoinType.XMR
    }
}