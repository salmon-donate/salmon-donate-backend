--liquibase formatted sql

--changeset chekist32:add_time_zone_column_users_table
ALTER TABLE users
ADD COLUMN time_zone VARCHAR(255) NOT NULL REFERENCES time_zones(name) DEFAULT 'America/New_York';
--rollback ALTER TABLE users DROP COLUMN time_zone;