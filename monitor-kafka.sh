#!/bin/bash

# Kafka Event Monitor Script
# Usage: ./monitor-kafka.sh [topic] [mode]
# Examples:
#   ./monitor-kafka.sh success          # Monitor payment.success from beginning
#   ./monitor-kafka.sh failed new       # Monitor payment.failed (new events only)
#   ./monitor-kafka.sh all              # Monitor all topics in separate windows

KAFKA_CONTAINER="kafka"
BOOTSTRAP_SERVER="localhost:9092"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to monitor a single topic
monitor_topic() {
    local topic=$1
    local mode=$2
    
    echo -e "${GREEN}📡 Monitoring Kafka topic: ${YELLOW}payment.${topic}${NC}"
    echo -e "${BLUE}Press Ctrl+C to stop${NC}\n"
    
    if [ "$mode" = "new" ]; then
        # Monitor only new events
        docker exec -it $KAFKA_CONTAINER kafka-console-consumer \
            --bootstrap-server $BOOTSTRAP_SERVER \
            --topic "payment.${topic}" \
            --property print.timestamp=true \
            --property print.key=true
    else
        # Monitor from beginning
        docker exec -it $KAFKA_CONTAINER kafka-console-consumer \
            --bootstrap-server $BOOTSTRAP_SERVER \
            --topic "payment.${topic}" \
            --from-beginning \
            --property print.timestamp=true \
            --property print.key=true
    fi
}

# Function to list all topics
list_topics() {
    echo -e "${GREEN}📋 Available Kafka Topics:${NC}"
    docker exec -it $KAFKA_CONTAINER kafka-topics \
        --list \
        --bootstrap-server $BOOTSTRAP_SERVER
}

# Function to describe a topic
describe_topic() {
    local topic=$1
    echo -e "${GREEN}📊 Topic Details: ${YELLOW}payment.${topic}${NC}"
    docker exec -it $KAFKA_CONTAINER kafka-topics \
        --describe \
        --topic "payment.${topic}" \
        --bootstrap-server $BOOTSTRAP_SERVER
}

# Main script logic
case "$1" in
    "success")
        monitor_topic "success" "$2"
        ;;
    "failed")
        monitor_topic "failed" "$2"
        ;;
    "refunded")
        monitor_topic "refunded" "$2"
        ;;
    "list")
        list_topics
        ;;
    "describe")
        if [ -z "$2" ]; then
            echo -e "${RED}Error: Please specify topic name${NC}"
            echo "Usage: ./monitor-kafka.sh describe [success|failed|refunded]"
            exit 1
        fi
        describe_topic "$2"
        ;;
    "all")
        echo -e "${YELLOW}Opening 3 terminal windows to monitor all payment topics...${NC}"
        echo -e "${BLUE}Note: This requires macOS Terminal or iTerm2${NC}\n"
        
        # Open new terminal windows (macOS)
        osascript -e 'tell application "Terminal" to do script "cd \"'$(pwd)'\" && ./monitor-kafka.sh success"'
        sleep 1
        osascript -e 'tell application "Terminal" to do script "cd \"'$(pwd)'\" && ./monitor-kafka.sh failed"'
        sleep 1
        osascript -e 'tell application "Terminal" to do script "cd \"'$(pwd)'\" && ./monitor-kafka.sh refunded"'
        
        echo -e "${GREEN}✅ Opened 3 monitoring windows${NC}"
        ;;
    "help"|"--help"|"-h"|"")
        echo -e "${GREEN}Kafka Event Monitor - Payment Service${NC}"
        echo ""
        echo "Usage: ./monitor-kafka.sh [command] [options]"
        echo ""
        echo "Commands:"
        echo "  success              Monitor payment.success topic"
        echo "  failed               Monitor payment.failed topic"
        echo "  refunded             Monitor payment.refunded topic"
        echo "  all                  Open 3 windows monitoring all topics (macOS only)"
        echo "  list                 List all Kafka topics"
        echo "  describe [topic]     Show topic details"
        echo "  help                 Show this help message"
        echo ""
        echo "Options:"
        echo "  new                  Monitor only new events (don't show history)"
        echo ""
        echo "Examples:"
        echo "  ./monitor-kafka.sh success              # Monitor success events from beginning"
        echo "  ./monitor-kafka.sh failed new           # Monitor only new failed events"
        echo "  ./monitor-kafka.sh all                  # Monitor all topics in separate windows"
        echo "  ./monitor-kafka.sh list                 # List all topics"
        echo "  ./monitor-kafka.sh describe success     # Show success topic details"
        echo ""
        ;;
    *)
        echo -e "${RED}Error: Unknown command '$1'${NC}"
        echo "Run './monitor-kafka.sh help' for usage information"
        exit 1
        ;;
esac
