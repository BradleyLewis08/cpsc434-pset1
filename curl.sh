#!/bin/bash

# This function will run when the script receives a SIGINT (Ctrl+C)
trap 'echo -e "\nScript interrupted" ; exit 1' SIGINT

# Initialize a counter
counter=0

# Define arrays of user-agents, accept headers, and files
USER_AGENTS=("Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1" "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.77 Safari/537.36")
ACCEPT_HEADERS=("text/html" "image/jpeg" "*/*")
FILES=("index.html" "about.html" "contact.html")

# Infinite loop to keep executing the command until interrupted
while :
do
    # Increment the counter
    ((counter++))

    # Randomly select user-agent, accept header, and file
    RANDOM_USER_AGENT=${USER_AGENTS[$RANDOM % ${#USER_AGENTS[@]}]}
    RANDOM_ACCEPT_HEADER=${ACCEPT_HEADERS[$RANDOM % ${#ACCEPT_HEADERS[@]}]}
    RANDOM_FILE=${FILES[$RANDOM % ${#FILES[@]}]}

    # Print details of the request
    echo "Request: #$counter"
    echo "User-Agent: $RANDOM_USER_AGENT"
    echo "Accept: $RANDOM_ACCEPT_HEADER"
    echo "File: $RANDOM_FILE"

    curl -v -X GET \
    -H "Host: localhost" \
    -H "User-Agent: $RANDOM_USER_AGENT" \
    -H "Accept: $RANDOM_ACCEPT_HEADER" \
    -H "Connection: close" \
    http://localhost:6789/load

    # Sleep for a short period (e.g., 1 second) to avoid spamming the server too fast
    sleep 1000
done
