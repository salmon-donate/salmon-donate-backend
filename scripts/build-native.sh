#!/bin/bash

set -a
source .env.build
set +a

docker compose -f docker-compose-build.yml up -d

./mvnw dependency:go-offline
./mvnw clean install package -Dnative -Dliquibase.skip=true -DskipTests=true

docker compose -f docker-compose-build.yml down