--liquibase formatted sql

-- changeset chekist32:create-currency_type-enum
CREATE TYPE currency_type AS ENUM ('USD');
-- rollback DROP TYPE currency_type;

--changeset chekist32:create_user_table
CREATE TABLE users(
    "id" UUID PRIMARY KEY, -- From KEYCLOAK
    "bio" TEXT,
    "avatar_url" TEXT,
    "currency" currency_type NOT NULL DEFAULT 'USD'
);
-- rollback DROP TABLE users CASCADE;
