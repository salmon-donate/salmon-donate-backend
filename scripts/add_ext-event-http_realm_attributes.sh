#!/bin/bash

BACKEND_BASE_URL="http://172.18.0.1:8080"
BACKEND_SHARED_SECRET="60gsElBpzZ8n7CETtVG9vg=="

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

PAYLOAD=$(jq -n \
  --arg targetUri "${BACKEND_BASE_URL}/api/v1/webhook/keycloak_event_webhook" \
  --arg sharedSecret "${BACKEND_SHARED_SECRET}" \
  '{
    attributes: {
      "_providerConfig.ext-event-http": {
        targetUri: $targetUri,
        sharedSecret: $sharedSecret
      } | @json
    }
  }')

RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT \
  "${KEYCLOAK_BASE_URL}/admin/realms/${KEYCLOAK_REALM}" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "${PAYLOAD}")

if [[ "$RESPONSE" == 204 ]]
then
  echo "Custom realm attributes updated successfully."
  exit 0
else
  echo "Failed to update custom realm attributes. HTTP Status: $RESPONSE"
  exit 2
fi