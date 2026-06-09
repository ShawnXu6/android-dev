#!/usr/bin/env bash
cd "$(dirname "$0")" || exit 1
./gradlew :app:compileDebugKotlin > /tmp/compile_out.txt 2>&1
echo "COMPILE_EXIT=$?"
./gradlew :app:testDebugUnitTest > /tmp/test_out.txt 2>&1
echo "TEST_EXIT=$?"
