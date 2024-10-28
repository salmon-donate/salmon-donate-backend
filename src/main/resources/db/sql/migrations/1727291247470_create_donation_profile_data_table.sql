--liquibase formatted sql

-- changeset chekist32:create-confirmation_type-enum
CREATE TYPE confirmation_type AS ENUM ('UNCONFIRMED', 'PARTIALLY_CONFIRMED', 'CONFIRMED');
-- rollback DROP TYPE confirmation_type;

-- changeset chekist32:create-crypto_type-enum
CREATE TYPE crypto_type AS ENUM ('XMR');
-- rollback DROP TYPE crypto_type;

-- changeset chekist32:create-donation_profile_data-table
CREATE TABLE donation_profile_data(
    "user_id" UUID PRIMARY KEY REFERENCES users(id),
    "min_amount" DOUBLE PRECISION NOT NULL DEFAULT 0.1,
    "timeout" SMALLINT NOT NULL DEFAULT 40*60,
    "confirmation_type" confirmation_type NOT NULL DEFAULT 'UNCONFIRMED',
    "notification_token" UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    "enabled_crypto" crypto_type[] NOT NULL DEFAULT ARRAY[]::crypto_type[]
);
-- rollback DROP TABLE donation_profile_data;