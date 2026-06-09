#!/bin/bash
# ════════════════════════════════════════════════════════════════════════════
# RESUMEFORGE: Unit Test Runner
# Runs all unit and integration tests with coverage reporting
# ════════════════════════════════════════════════════════════════════════════

set -e

echo "════════════════════════════════════════════════════════════════"
echo "🧪 ResumeForge Unit Test Suite"
echo "════════════════════════════════════════════════════════════════"
echo ""

cd resume-service

echo "1️⃣  Running all tests..."
echo ""

mvn clean test -X 2>&1 | tail -100

echo ""
echo "2️⃣  Generating coverage report..."
echo ""

mvn jacoco:report

echo ""
echo "════════════════════════════════════════════════════════════════"
echo "✅ Tests completed!"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "Coverage report: resume-service/target/site/jacoco/index.html"
echo ""
