--liquibase formatted sql

--changeset chekist32:create_donations_table
CREATE TABLE donations(
    "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "from" VARCHAR(255) NOT NULL,
    "message" TEXT,
    "user_id" UUID NOT NULL REFERENCES users(id),
    "payment_id" UUID NOT NULL UNIQUE,
    "shown_at" TIMESTAMP WITH TIME ZONE
);
--rollback DROP TABLE donations;