#!/bin/bash

isNativeBuild=false

while getopts "n" option; do
    case $option in
        n) isNativeBuild=true;;
        *) ;;
    esac
done

set -a
source .env.build
set +a

if [[ "$isNativeBuild" = true ]]
then
  ./mvnw clean install package -Dnative -Dliquibase.skip=true -DskipTests=true
else
  ./mvnw clean install package -Dquarkus.package.type=fast-jar -Dliquibase.skip=true -DskipTests=true
fi