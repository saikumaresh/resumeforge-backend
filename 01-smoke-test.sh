#!/bin/bash
# ════════════════════════════════════════════════════════════════════════════
# RESUMEFORGE: Smoke Test
# Quick validation that services start and respond
# ════════════════════════════════════════════════════════════════════════════

set -e

echo "════════════════════════════════════════════════════════════════"
echo "🔥 ResumeForge Smoke Test"
echo "════════════════════════════════════════════════════════════════"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if service is running
check_health() {
  local PORT=$1
  local SERVICE=$2

  echo -n "Checking $SERVICE (port $PORT)... "

  if curl -s "http://localhost:$PORT/actuator/health" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ UP${NC}"
    return 0
  else
    echo -e "${RED}❌ DOWN${NC}"
    return 1
  fi
}

echo "1️⃣  Waiting 5 seconds for services to start..."
sleep 5

echo ""
echo "2️⃣  Checking service health..."
ALL_PASS=true

if ! check_health 8080 "API Gateway"; then ALL_PASS=false; fi
if ! check_health 8081 "Resume Service"; then ALL_PASS=false; fi

echo ""
if [ "$ALL_PASS" = true ]; then
  echo -e "${GREEN}════════════════════════════════════════════════════════════════${NC}"
  echo -e "${GREEN}✅ SMOKE TEST PASSED - All services are UP${NC}"
  echo -e "${GREEN}════════════════════════════════════════════════════════════════${NC}"
  exit 0
else
  echo -e "${RED}════════════════════════════════════════════════════════════════${NC}"
  echo -e "${RED}❌ SMOKE TEST FAILED - Some services are DOWN${NC}"
  echo -e "${RED}════════════════════════════════════════════════════════════════${NC}"
  exit 1
fi
