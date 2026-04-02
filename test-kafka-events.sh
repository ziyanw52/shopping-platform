#!/bin/bash

# Test Kafka Events Script
# This script helps verify that Kafka events are being published correctly

echo "🧪 Kafka Event Testing Script"
echo "=============================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Kafka is running
echo -e "${BLUE}1. Checking Kafka status...${NC}"
if docker ps | grep -q kafka; then
    echo -e "${GREEN}✅ Kafka is running${NC}"
else
    echo -e "${RED}❌ Kafka is not running${NC}"
    echo "Start Kafka with: docker-compose up -d kafka"
    exit 1
fi

echo ""

# List all topics
echo -e "${BLUE}2. Listing all Kafka topics...${NC}"
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092
echo ""

# Check if payment topics exist
echo -e "${BLUE}3. Checking payment topics...${NC}"
TOPICS=$(docker exec kafka kafka-topics --list --bootstrap-server localhost:9092)

if echo "$TOPICS" | grep -q "payment.success"; then
    echo -e "${GREEN}✅ payment.success topic exists${NC}"
else
    echo -e "${YELLOW}⚠️  payment.success topic not found (will be created on first event)${NC}"
fi

if echo "$TOPICS" | grep -q "payment.failed"; then
    echo -e "${GREEN}✅ payment.failed topic exists${NC}"
else
    echo -e "${YELLOW}⚠️  payment.failed topic not found (will be created on first event)${NC}"
fi

if echo "$TOPICS" | grep -q "payment.refunded"; then
    echo -e "${GREEN}✅ payment.refunded topic exists${NC}"
else
    echo -e "${YELLOW}⚠️  payment.refunded topic not found (will be created on first event)${NC}"
fi

echo ""

# Count messages in each topic
echo -e "${BLUE}4. Counting messages in payment topics...${NC}"

for topic in "payment.success" "payment.failed" "payment.refunded"; do
    if echo "$TOPICS" | grep -q "$topic"; then
        COUNT=$(docker exec kafka kafka-run-class kafka.tools.GetOffsetShell \
            --broker-list localhost:9092 \
            --topic "$topic" \
            --time -1 2>/dev/null | awk -F ":" '{sum += $3} END {print sum}')
        
        if [ -z "$COUNT" ]; then
            COUNT=0
        fi
        
        if [ "$COUNT" -gt 0 ]; then
            echo -e "${GREEN}  $topic: $COUNT messages${NC}"
        else
            echo -e "${YELLOW}  $topic: 0 messages${NC}"
        fi
    fi
done

echo ""
echo -e "${BLUE}5. Instructions for monitoring events:${NC}"
echo ""
echo "To monitor payment.success events:"
echo -e "${YELLOW}  docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic payment.success --from-beginning${NC}"
echo ""
echo "To monitor payment.failed events:"
echo -e "${YELLOW}  docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic payment.failed --from-beginning${NC}"
echo ""
echo "To monitor payment.refunded events:"
echo -e "${YELLOW}  docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic payment.refunded --from-beginning${NC}"
echo ""
echo "Or use the helper script:"
echo -e "${YELLOW}  ./monitor-kafka.sh success${NC}"
echo -e "${YELLOW}  ./monitor-kafka.sh failed${NC}"
echo -e "${YELLOW}  ./monitor-kafka.sh refunded${NC}"
echo ""

# Offer to start monitoring
echo -e "${BLUE}6. Would you like to start monitoring now?${NC}"
echo "Choose an option:"
echo "  1) Monitor payment.success"
echo "  2) Monitor payment.failed"
echo "  3) Monitor payment.refunded"
echo "  4) Monitor all (opens 3 terminals - macOS only)"
echo "  5) Exit"
echo ""
read -p "Enter choice [1-5]: " choice

case $choice in
    1)
        echo -e "${GREEN}Starting payment.success monitor...${NC}"
        docker exec -it kafka kafka-console-consumer \
            --bootstrap-server localhost:9092 \
            --topic payment.success \
            --from-beginning \
            --property print.timestamp=true
        ;;
    2)
        echo -e "${GREEN}Starting payment.failed monitor...${NC}"
        docker exec -it kafka kafka-console-consumer \
            --bootstrap-server localhost:9092 \
            --topic payment.failed \
            --from-beginning \
            --property print.timestamp=true
        ;;
    3)
        echo -e "${GREEN}Starting payment.refunded monitor...${NC}"
        docker exec -it kafka kafka-console-consumer \
            --bootstrap-server localhost:9092 \
            --topic payment.refunded \
            --from-beginning \
            --property print.timestamp=true
        ;;
    4)
        if [ "$(uname)" = "Darwin" ]; then
            echo -e "${GREEN}Opening 3 terminal windows...${NC}"
            osascript -e 'tell application "Terminal" to do script "cd \"'$(pwd)'\" && docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic payment.success --from-beginning --property print.timestamp=true"'
            sleep 1
            osascript -e 'tell application "Terminal" to do script "cd \"'$(pwd)'\" && docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic payment.failed --from-beginning --property print.timestamp=true"'
            sleep 1
            osascript -e 'tell application "Terminal" to do script "cd \"'$(pwd)'\" && docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic payment.refunded --from-beginning --property print.timestamp=true"'
            echo -e "${GREEN}✅ Opened 3 monitoring windows${NC}"
        else
            echo -e "${RED}This option is only available on macOS${NC}"
        fi
        ;;
    5)
        echo -e "${GREEN}Exiting...${NC}"
        exit 0
        ;;
    *)
        echo -e "${RED}Invalid choice${NC}"
        exit 1
        ;;
esac
