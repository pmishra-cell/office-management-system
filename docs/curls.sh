#!/bin/bash

BASE_URL="http://127.0.0.1:8080"

echo "=== Register user ==="
curl -i -X POST "$BASE_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "name": "Alice",
    "password": "StrongPass123!"
  }'

echo "\n\n=== Login user ==="
curl -i -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "StrongPass123!"
  }'

echo "\n\n=== Health check ==="
curl -i "$BASE_URL/actuator/health"

echo "\n\n=== Info endpoint ==="
curl -i "$BASE_URL/actuator/info"

echo "\n\n=== H2 console ==="
echo "Open: http://127.0.0.1:8080/h2-console"
echo "JDBC URL: jdbc:h2:mem:smartoffice"
echo "User: sa"
echo "Password: (empty)"
