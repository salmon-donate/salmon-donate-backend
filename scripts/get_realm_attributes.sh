#!/bin/bash

KEYCLOAK_BASE_URL="http://localhost:9080"
KEYCLOAK_REALM="salmon-donate"
KEYCLOAK_ADMIN_USER="admin"
KEYCLOAK_ADMIN_PASS="admin"
KEYCLOAK_CLIENT_ID="admin-cli"

ACCESS_TOKEN=$(curl -s -X POST "${KEYCLOAK_BASE_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${KEYCLOAK_ADMIN_USER}" \
  -d "password=${KEYCLOAK_ADMIN_PASS}" \
  -d 'grant_type=password' \
  -d "client_id=${KEYCLOAK_CLIENT_ID}" | jq -r '.access_token')

if [[ ! "$ACCESS_TOKEN" ]]
then
  echo "Failed to obtain access token"
  exit 1
fi

curl -X GET "${KEYCLOAK_BASE_URL}/admin/realms/${KEYCLOAK_REALM}" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json"