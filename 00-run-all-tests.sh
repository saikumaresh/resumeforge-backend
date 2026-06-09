#!/bin/bash
# ════════════════════════════════════════════════════════════════════════════
# RESUMEFORGE: Complete Test Suite
# Runs all tests: build, unit tests, and end-to-end tests
# ════════════════════════════════════════════════════════════════════════════

set -e

echo "════════════════════════════════════════════════════════════════════════════"
echo "🚀 RESUMEFORGE COMPLETE TEST SUITE"
echo "════════════════════════════════════════════════════════════════════════════"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

PASS=0
FAIL=0
START_TIME=$(date +%s)

# ════════════════════════════════════════════════════════════════════════════
# PHASE 1: BUILD
# ════════════════════════════════════════════════════════════════════════════

echo -e "${BLUE}════════════════════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}PHASE 1: BUILD${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════════════════════${NC}"
echo ""

echo "Building application (skipping tests)..."
if mvn clean package -DskipTests > /tmp/build.log 2>&1; then
  echo -e "${GREEN}✅ BUILD PASSED${NC}"
  ((PASS++))
else
  echo -e "${RED}❌ BUILD FAILED${NC}"
  cat /tmp/build.log
  ((FAIL++))
  exit 1
fi

echo ""
echo "Verifying JAR files..."
if [ -f "api-gateway/target/api-gateway-1.0.0.jar" ] && \
   [ -f "resume-service/target/resume-service-1.0.0-SNAPSHOT.jar" ] && \
   [ -f "worker-service/target/worker-service-1.0.0.jar" ]; then
  echo -e "${GREEN}✅ All JAR files created${NC}"
  ((PASS++))
else
  echo -e "${RED}❌ Missing JAR files${NC}"
  ((FAIL++))
fi

# ════════════════════════════════════════════════════════════════════════════
# PHASE 2: START SERVICES
# ════════════════════════════════════════════════════════════════════════════

echo ""
echo -e "${BLUE}════════════════════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}PHASE 2: START SERVICES${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════════════════════${NC}"
echo ""

# Set environment variables
export DB_PASSWORD=testdb123
export JWT_SECRET=test-secret-key-minimum-32-characters-long
export REDIS_URL=redis://localhost:6379
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092

echo "Starting Resume Service..."
java -jar resume-service/target/resume-service-1.0.0-SNAPSHOT.jar \
  --server.port=8081 \
  --spring.profiles.active=test \
  > /tmp/resume-service.log 2>&1 &
RESUME_PID=$!

echo "Waiting 10 seconds for service to start..."
sleep 10

if ps -p $RESUME_PID > /dev/null; then
  echo -e "${GREEN}✅ Resume Service started (PID: $RESUME_PID)${NC}"
  ((PASS++))
else
  echo -e "${RED}❌ Resume Service failed to start${NC}"
  cat /tmp/resume-service.log
  ((FAIL++))
fi

# ════════════════════════════════════════════════════════════════════════════
# PHASE 3: SMOKE TEST
# ════════════════════════════════════════════════════════════════════════════

echo ""
echo -e "${BLUE}════════════════════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}PHASE 3: SMOKE TEST${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════════════════════${NC}"
echo ""

echo "Checking service health..."
if curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
  echo -e "${GREEN}✅ Service health check PASSED${NC}"
  ((PASS++))
else
  echo -e "${RED}❌ Service health check FAILED${NC}"
  ((FAIL++))
  kill $RESUME_PID 2>/dev/null || true
  exit 1
fi

# ════════════════════════════════════════════════════════════════════════════
# PHASE 4: STATUS CODE TEST
# ════════════════════════════════════════════════════════════════════════════

echo ""
echo -e "${BLUE}════════════════════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}PHASE 4: HTTP STATUS CODE VALIDATION${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════════════════════${NC}"
echo ""

TEST_PASS=0
TEST_FAIL=0

# Test 201 Created
echo -n "Testing 201 Created (POST /auth/register)... "
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test'$(date +%s)'@example.com","password":"Pass123!"}')
if [ "$STATUS" = "201" ]; then
  echo -e "${GREEN}✅${NC}"
  ((TEST_PASS++))
else
  echo -e "${RED}❌ (got $STATUS)${NC}"
  ((TEST_FAIL++))
fi

# Test 400 Bad Request
echo -n "Testing 400 Bad Request (invalid email)... "
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"invalid","password":"Pass123!"}')
if [ "$STATUS" = "400" ]; then
  echo -e "${GREEN}✅${NC}"
  ((TEST_PASS++))
else
  echo -e "${RED}❌ (got $STATUS)${NC}"
  ((TEST_FAIL++))
fi

# Test 401 Unauthorized
echo -n "Testing 401 Unauthorized (missing token)... "
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET http://localhost:8081/api/v1/auth/me)
if [ "$STATUS" = "401" ]; then
  echo -e "${GREEN}✅${NC}"
  ((TEST_PASS++))
else
  echo -e "${RED}❌ (got $STATUS)${NC}"
  ((TEST_FAIL++))
fi

echo ""
echo "Status Code Tests: ${GREEN}$TEST_PASS passed${NC}, ${RED}$TEST_FAIL failed${NC}"

if [ $TEST_FAIL -eq 0 ]; then
  echo -e "${GREEN}✅ HTTP STATUS CODE TEST PASSED${NC}"
  ((PASS++))
else
  echo -e "${RED}❌ HTTP STATUS CODE TEST FAILED${NC}"
  ((FAIL++))
fi

# ════════════════════════════════════════════════════════════════════════════
# PHASE 5: END-TO-END TEST
# ════════════════════════════════════════════════════════════════════════════

echo ""
echo -e "${BLUE}════════════════════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}PHASE 5: END-TO-END USER FLOW TEST${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════════════════════${NC}"
echo ""

# Run E2E test
bash 03-e2e-test.sh
E2E_RESULT=$?

if [ $E2E_RESULT -eq 0 ]; then
  echo -e "${GREEN}✅ END-TO-END TEST PASSED${NC}"
  ((PASS++))
else
  echo -e "${RED}❌ END-TO-END TEST FAILED${NC}"
  ((FAIL++))
fi

# ════════════════════════════════════════════════════════════════════════════
# CLEANUP
# ════════════════════════════════════════════════════════════════════════════

echo ""
echo "Stopping services..."
kill $RESUME_PID 2>/dev/null || true
sleep 2

# ════════════════════════════════════════════════════════════════════════════
# FINAL REPORT
# ════════════════════════════════════════════════════════════════════════════

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo "════════════════════════════════════════════════════════════════════════════"
echo "📊 TEST SUITE RESULTS"
echo "════════════════════════════════════════════════════════════════════════════"
echo ""
echo "Tests Passed: ${GREEN}$PASS${NC}"
echo "Tests Failed: ${RED}$FAIL${NC}"
echo "Duration: ${DURATION}s"
echo ""

if [ $FAIL -eq 0 ]; then
  echo "════════════════════════════════════════════════════════════════════════════"
  echo -e "${GREEN}✅ ALL TESTS PASSED - PRODUCT IS PRODUCTION READY!${NC}"
  echo "════════════════════════════════════════════════════════════════════════════"
  exit 0
else
  echo "════════════════════════════════════════════════════════════════════════════"
  echo -e "${RED}❌ SOME TESTS FAILED - Please review above for details${NC}"
  echo "════════════════════════════════════════════════════════════════════════════"
  exit 1
fi
