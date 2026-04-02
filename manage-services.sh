#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

show_menu() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  Shopping Platform Service Manager${NC}"
    echo -e "${BLUE}========================================${NC}\n"
    echo "1. Start all services"
    echo "2. Stop all services"
    echo "3. Restart all services"
    echo "4. Check service status"
    echo "5. Check connections"
    echo "6. View logs (all services)"
    echo "7. View logs (specific service)"
    echo "8. Clean up (stop and remove volumes)"
    echo "9. Exit"
    echo ""
    echo -n "Select an option [1-9]: "
}

start_services() {
    echo -e "${YELLOW}Starting all services...${NC}"
    docker-compose up -d
    echo -e "${GREEN}Services started. Waiting for health checks...${NC}"
    sleep 5
    docker-compose ps
}

stop_services() {
    echo -e "${YELLOW}Stopping all services...${NC}"
    docker-compose down
    echo -e "${GREEN}Services stopped.${NC}"
}

restart_services() {
    echo -e "${YELLOW}Restarting all services...${NC}"
    docker-compose restart
    echo -e "${GREEN}Services restarted.${NC}"
}

check_status() {
    echo -e "${YELLOW}Service Status:${NC}"
    docker-compose ps
}

check_connections() {
    if [ -f "./check-connections.sh" ]; then
        bash ./check-connections.sh
    else
        echo -e "${RED}check-connections.sh not found!${NC}"
    fi
}

view_all_logs() {
    echo -e "${YELLOW}Showing logs for all services (Ctrl+C to exit)...${NC}"
    docker-compose logs -f
}

view_service_logs() {
    echo ""
    echo "Available services:"
    echo "  - mysql"
    echo "  - mongo"
    echo "  - cassandra"
    echo "  - zookeeper"
    echo "  - kafka"
    echo "  - auth-service"
    echo "  - account-service"
    echo "  - item-service"
    echo "  - order-service"
    echo "  - payment-service"
    echo "  - frontend"
    echo ""
    echo -n "Enter service name: "
    read service_name
    echo -e "${YELLOW}Showing logs for $service_name (Ctrl+C to exit)...${NC}"
    docker-compose logs -f "$service_name"
}

cleanup() {
    echo -e "${RED}WARNING: This will stop all services and delete all data!${NC}"
    echo -n "Are you sure? (yes/no): "
    read confirm
    if [ "$confirm" = "yes" ]; then
        echo -e "${YELLOW}Cleaning up...${NC}"
        docker-compose down -v
        echo -e "${GREEN}Cleanup complete.${NC}"
    else
        echo -e "${YELLOW}Cleanup cancelled.${NC}"
    fi
}

# Main loop
while true; do
    show_menu
    read choice
    echo ""
    
    case $choice in
        1) start_services ;;
        2) stop_services ;;
        3) restart_services ;;
        4) check_status ;;
        5) check_connections ;;
        6) view_all_logs ;;
        7) view_service_logs ;;
        8) cleanup ;;
        9) echo -e "${GREEN}Goodbye!${NC}"; exit 0 ;;
        *) echo -e "${RED}Invalid option. Please try again.${NC}" ;;
    esac
    
    echo ""
    echo -n "Press Enter to continue..."
    read
    clear
done
