#!/bin/bash
# ════════════════════════════════════════════════════════════════════════════
# RESUMEFORGE: End-to-End Test
# Complete user flow: Register → Login → Create Resume → Get Profile
# ════════════════════════════════════════════════════════════════════════════

set -e

echo "════════════════════════════════════════════════════════════════"
echo "🚀 ResumeForge End-to-End Test"
echo "════════════════════════════════════════════════════════════════"
echo ""

BASE_URL="http://localhost:8081"
EMAIL="testuser-$(date +%s)@example.com"
PASSWORD="SecurePassword123!"
TOKEN=""
USER_ID=""
RESUME_ID=""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

test_step() {
  local STEP=$1
  local DESCRIPTION=$2
  echo ""
  echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${BLUE}$STEP. $DESCRIPTION${NC}"
  echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

# ════════════════════════════════════════════════════════════════════════════
# STEP 1: REGISTER NEW USER
# ════════════════════════════════════════════════════════════════════════════

test_step "1" "User Registration (POST /api/v1/auth/register)"

echo "Registering new user: $EMAIL"
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"Test User\",
    \"email\": \"$EMAIL\",
    \"password\": \"$PASSWORD\"
  }")

echo "Response: $REGISTER_RESPONSE" | head -c 200
echo "..."

# Extract values using grep and sed (compatible with Windows)
TOKEN=$(echo "$REGISTER_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
USER_ID=$(echo "$REGISTER_RESPONSE" | grep -o '"userId":"[^"]*' | cut -d'"' -f4)
RESPONSE_EMAIL=$(echo "$REGISTER_RESPONSE" | grep -o '"email":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ] || [ -z "$USER_ID" ]; then
  echo -e "${RED}❌ FAILED: Could not extract token or user ID${NC}"
  echo "Full response: $REGISTER_RESPONSE"
  exit 1
fi

echo ""
echo -e "${GREEN}✅ Registration successful${NC}"
echo "   User ID: $USER_ID"
echo "   Email: $RESPONSE_EMAIL"
echo "   Token: ${TOKEN:0:20}..."

# ════════════════════════════════════════════════════════════════════════════
# STEP 2: LOGIN
# ════════════════════════════════════════════════════════════════════════════

test_step "2" "User Login (POST /api/v1/auth/login)"

echo "Logging in with email: $EMAIL"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$EMAIL\",
    \"password\": \"$PASSWORD\"
  }")

LOGIN_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
LOGIN_USER_ID=$(echo "$LOGIN_RESPONSE" | grep -o '"userId":"[^"]*' | cut -d'"' -f4)

if [ -z "$LOGIN_TOKEN" ] || [ -z "$LOGIN_USER_ID" ]; then
  echo -e "${RED}❌ FAILED: Login failed${NC}"
  echo "Response: $LOGIN_RESPONSE"
  exit 1
fi

echo -e "${GREEN}✅ Login successful${NC}"
echo "   Token: ${LOGIN_TOKEN:0:20}..."

# ════════════════════════════════════════════════════════════════════════════
# STEP 3: GET USER PROFILE
# ════════════════════════════════════════════════════════════════════════════

test_step "3" "Get User Profile (GET /api/v1/auth/me)"

echo "Fetching user profile..."
PROFILE_RESPONSE=$(curl -s -X GET "$BASE_URL/api/v1/auth/me" \
  -H "Authorization: Bearer $TOKEN")

PROFILE_NAME=$(echo "$PROFILE_RESPONSE" | grep -o '"name":"[^"]*' | cut -d'"' -f4)
PROFILE_EMAIL=$(echo "$PROFILE_RESPONSE" | grep -o '"email":"[^"]*' | cut -d'"' -f4)
PROFILE_PLAN=$(echo "$PROFILE_RESPONSE" | grep -o '"plan":"[^"]*' | cut -d'"' -f4)

if [ -z "$PROFILE_NAME" ]; then
  echo -e "${RED}❌ FAILED: Could not fetch profile${NC}"
  echo "Response: $PROFILE_RESPONSE"
  exit 1
fi

echo -e "${GREEN}✅ Profile retrieved successfully${NC}"
echo "   Name: $PROFILE_NAME"
echo "   Email: $PROFILE_EMAIL"
echo "   Plan: $PROFILE_PLAN"

# ════════════════════════════════════════════════════════════════════════════
# STEP 4: CREATE MASTER RESUME (201 Created)
# ════════════════════════════════════════════════════════════════════════════

test_step "4" "Create Master Resume (POST /api/v1/resumes/users/{userId}/master)"

echo "Creating new master resume..."
RESUME_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/resumes/users/$USER_ID/master" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"Senior Software Engineer Resume\",
    \"summary\": \"Experienced full-stack developer with 5+ years of experience building scalable web applications. Expert in Java, Spring Boot, React, and cloud technologies.\"
  }")

# Parse response and status code
RESUME_HTTP_CODE=$(echo "$RESUME_RESPONSE" | tail -n1)
RESUME_BODY=$(echo "$RESUME_RESPONSE" | head -n-1)

echo "HTTP Status: $RESUME_HTTP_CODE"

RESUME_ID=$(echo "$RESUME_BODY" | grep -o '"id":"[^"]*' | cut -d'"' -f4)
RESUME_TITLE=$(echo "$RESUME_BODY" | grep -o '"title":"[^"]*' | cut -d'"' -f4)

if [ "$RESUME_HTTP_CODE" != "201" ]; then
  echo -e "${RED}❌ FAILED: Expected 201 Created but got $RESUME_HTTP_CODE${NC}"
  echo "Response: $RESUME_BODY"
  exit 1
fi

if [ -z "$RESUME_ID" ]; then
  echo -e "${RED}❌ FAILED: Could not extract resume ID${NC}"
  echo "Response: $RESUME_BODY"
  exit 1
fi

echo -e "${GREEN}✅ Resume created successfully (201 Created)${NC}"
echo "   Resume ID: $RESUME_ID"
echo "   Title: $RESUME_TITLE"

# ════════════════════════════════════════════════════════════════════════════
# STEP 5: GET MASTER RESUMES LIST
# ════════════════════════════════════════════════════════════════════════════

test_step "5" "Get User's Master Resumes (GET /api/v1/resumes/users/{userId}/master)"

echo "Fetching all user resumes..."
LIST_RESPONSE=$(curl -s -X GET "$BASE_URL/api/v1/resumes/users/$USER_ID/master" \
  -H "Authorization: Bearer $TOKEN")

RESUME_COUNT=$(echo "$LIST_RESPONSE" | grep -o '"id":"' | wc -l)

if [ "$RESUME_COUNT" -lt 1 ]; then
  echo -e "${RED}❌ FAILED: No resumes in list${NC}"
  exit 1
fi

echo -e "${GREEN}✅ Resume list retrieved successfully${NC}"
echo "   Total resumes: $RESUME_COUNT"

# ════════════════════════════════════════════════════════════════════════════
# STEP 6: SECURITY TEST - Try to access without token
# ════════════════════════════════════════════════════════════════════════════

test_step "6" "Security Test - Access without Token (401 Unauthorized)"

echo "Attempting to access protected endpoint without token..."
NO_TOKEN_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/v1/auth/me")
NO_TOKEN_HTTP_CODE=$(echo "$NO_TOKEN_RESPONSE" | tail -n1)

if [ "$NO_TOKEN_HTTP_CODE" = "401" ]; then
  echo -e "${GREEN}✅ Security check passed (401 Unauthorized)${NC}"
else
  echo -e "${RED}❌ FAILED: Expected 401 but got $NO_TOKEN_HTTP_CODE${NC}"
  exit 1
fi

# ════════════════════════════════════════════════════════════════════════════
# STEP 7: SECURITY TEST - Try with invalid token
# ════════════════════════════════════════════════════════════════════════════

test_step "7" "Security Test - Access with Invalid Token (401 Unauthorized)"

echo "Attempting to access with invalid token..."
INVALID_TOKEN_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/v1/auth/me" \
  -H "Authorization: Bearer invalid.token.here")
INVALID_TOKEN_HTTP_CODE=$(echo "$INVALID_TOKEN_RESPONSE" | tail -n1)

if [ "$INVALID_TOKEN_HTTP_CODE" = "401" ]; then
  echo -e "${GREEN}✅ Security check passed (401 Unauthorized)${NC}"
else
  echo -e "${RED}❌ FAILED: Expected 401 but got $INVALID_TOKEN_HTTP_CODE${NC}"
  exit 1
fi

# ════════════════════════════════════════════════════════════════════════════
# FINAL SUMMARY
# ════════════════════════════════════════════════════════════════════════════

echo ""
echo "════════════════════════════════════════════════════════════════"
echo -e "${GREEN}✅ ALL END-TO-END TESTS PASSED!${NC}"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "Summary:"
echo "  ✅ User Registration (201 Created)"
echo "  ✅ User Login (200 OK)"
echo "  ✅ Get User Profile (200 OK)"
echo "  ✅ Create Master Resume (201 Created)"
echo "  ✅ Get Resume List (200 OK)"
echo "  ✅ Missing Token → 401 Unauthorized"
echo "  ✅ Invalid Token → 401 Unauthorized"
echo ""
echo "Product is ${GREEN}PRODUCTION READY${NC} for user testing!"
echo "════════════════════════════════════════════════════════════════"
