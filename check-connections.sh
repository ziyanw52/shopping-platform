#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Shopping Platform Connection Checker${NC}"
echo -e "${BLUE}========================================${NC}\n"

# Function to check HTTP endpoint
check_http() {
    local name=$1
    local url=$2
    
    if curl -s -f -o /dev/null -w "%{http_code}" "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} $name: ${GREEN}Connected${NC} ($url)"
        return 0
    else
        echo -e "${RED}✗${NC} $name: ${RED}Not responding${NC} ($url)"
        return 1
    fi
}

# Function to check port
check_port() {
    local name=$1
    local host=$2
    local port=$3
    
    if nc -z -w 2 "$host" "$port" 2>/dev/null; then
        echo -e "${GREEN}✓${NC} $name: ${GREEN}Port $port open${NC}"
        return 0
    else
        echo -e "${RED}✗${NC} $name: ${RED}Port $port closed${NC}"
        return 1
    fi
}

# Check Docker Compose status
echo -e "${YELLOW}Docker Services Status:${NC}"
docker-compose ps
echo ""

# Check Microservices
echo -e "${YELLOW}Microservices Health:${NC}"
check_http "Auth Service" "http://localhost:8085/actuator/health"
check_http "Account Service" "http://localhost:8081/actuator/health"
check_http "Item Service" "http://localhost:8082/actuator/health"
check_http "Order Service" "http://localhost:8083/actuator/health"
check_http "Payment Service" "http://localhost:8084/actuator/health"
echo ""

# Check Infrastructure Services
echo -e "${YELLOW}Infrastructure Services:${NC}"
check_port "MySQL" "localhost" "3307"
check_port "MongoDB" "localhost" "27017"
check_port "Cassandra" "localhost" "9042"
check_port "Zookeeper" "localhost" "2181"
check_port "Kafka" "localhost" "9092"
echo ""

# Detailed Database Checks
echo -e "${YELLOW}Database Connectivity:${NC}"

# MySQL
if docker exec sp-mysql mysql -uroot -proot -e "SELECT 1" > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} MySQL: ${GREEN}Connected and queryable${NC}"
    docker exec sp-mysql mysql -uroot -proot -e "SHOW DATABASES;" 2>/dev/null | grep -E "account_db|order_db" > /dev/null
    if [ $? -eq 0 ]; then
        echo -e "  ${GREEN}→${NC} Application databases exist"
    fi
else
    echo -e "${RED}✗${NC} MySQL: ${RED}Cannot connect${NC}"
fi

# MongoDB
if docker exec sp-mongo mongosh --quiet --eval "db.adminCommand('ping').ok" > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} MongoDB: ${GREEN}Connected and queryable${NC}"
else
    echo -e "${RED}✗${NC} MongoDB: ${RED}Cannot connect${NC}"
fi

# Cassandra
if docker exec cassandra cqlsh -e "DESCRIBE CLUSTER" > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Cassandra: ${GREEN}Connected and queryable${NC}"
else
    echo -e "${RED}✗${NC} Cassandra: ${RED}Cannot connect (may still be initializing)${NC}"
fi

echo ""

# Kafka Topics
echo -e "${YELLOW}Kafka Topics:${NC}"
if docker exec kafka kafka-topics --list --bootstrap-server localhost:9092 > /dev/null 2>&1; then
    topics=$(docker exec kafka kafka-topics --list --bootstrap-server localhost:9092 2>/dev/null)
    if [ -z "$topics" ]; then
        echo -e "${YELLOW}⚠${NC} Kafka: ${YELLOW}Connected but no topics created yet${NC}"
    else
        echo -e "${GREEN}✓${NC} Kafka: ${GREEN}Connected${NC}"
        echo "$topics" | while read topic; do
            echo -e "  ${GREEN}→${NC} $topic"
        done
    fi
else
    echo -e "${RED}✗${NC} Kafka: ${RED}Cannot connect${NC}"
fi

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Connection check complete!${NC}"
echo -e "${BLUE}========================================${NC}"
