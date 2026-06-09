#!/bin/bash
# ════════════════════════════════════════════════════════════════════════════
# RESUMEFORGE: HTTP Status Code Validation
# Verifies all endpoints return correct HTTP status codes
# ════════════════════════════════════════════════════════════════════════════

set -e

echo "════════════════════════════════════════════════════════════════"
echo "📊 ResumeForge HTTP Status Code Test"
echo "════════════════════════════════════════════════════════════════"
echo ""

BASE_URL="http://localhost:8081"
PASS=0
FAIL=0

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

test_endpoint() {
  local METHOD=$1
  local ENDPOINT=$2
  local EXPECTED_STATUS=$3
  local DESCRIPTION=$4
  local DATA=$5

  echo -n "Testing $METHOD $ENDPOINT ... "

  if [ -z "$DATA" ]; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X $METHOD "$BASE_URL$ENDPOINT")
  else
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X $METHOD "$BASE_URL$ENDPOINT" \
      -H "Content-Type: application/json" \
      -d "$DATA")
  fi

  if [ "$STATUS" = "$EXPECTED_STATUS" ]; then
    echo -e "${GREEN}✅ $STATUS (expected $EXPECTED_STATUS)${NC} - $DESCRIPTION"
    ((PASS++))
  else
    echo -e "${RED}❌ $STATUS (expected $EXPECTED_STATUS)${NC} - $DESCRIPTION"
    ((FAIL++))
  fi
}

echo "Testing Health & Status Codes..."
echo ""

# 1. Health check (200 OK)
test_endpoint "GET" "/actuator/health" "200" "Health check"

echo ""
echo "Testing Authentication Endpoints..."
echo ""

# 2. Register - 201 Created
test_endpoint "POST" "/api/v1/auth/register" "201" "Register new user" \
  '{"name":"Test User","email":"testuser'$(date +%s)'@example.com","password":"SecurePass123!"}'

# 3. Register with invalid email - 400 Bad Request
test_endpoint "POST" "/api/v1/auth/register" "400" "Register with invalid email" \
  '{"name":"Test","email":"invalid","password":"Pass123!"}'

# 4. Login with invalid token - 401 Unauthorized
test_endpoint "GET" "/api/v1/auth/me" "401" "Access protected endpoint without token"

echo ""
echo "════════════════════════════════════════════════════════════════"
echo "Results: ${GREEN}$PASS passed${NC}, ${RED}$FAIL failed${NC}"
echo "════════════════════════════════════════════════════════════════"
echo ""

if [ $FAIL -eq 0 ]; then
  echo -e "${GREEN}✅ All HTTP status code tests PASSED${NC}"
  exit 0
else
  echo -e "${RED}❌ Some HTTP status code tests FAILED${NC}"
  exit 1
fi
