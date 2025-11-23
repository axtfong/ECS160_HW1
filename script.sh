#!/bin/bash

# Script to compile and run all projects for HW2

set -e

echo "=== Building Persistence Framework ==="
cd persistence-framework
mvn clean install
cd ..

echo "=== Building Microservice Framework ==="
cd microservice-framework
mvn clean install
cd ..

echo "=== Building Application ==="
cd application
mvn clean compile
cd ..

echo "=== Running Tests ==="
cd application
mvn test
cd ..

echo "=== Build Complete ==="

