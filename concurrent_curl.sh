#!/bin/bash

# Function to make a curl request
make_request() {
  curl -i -X GET http://localhost:6789/load
}

# Start multiple requests in the background
for i in {1..10} # Replace 10 with the number of requests you want to make
do
  make_request & # This will start the request in the background
done

# Wait for all background jobs to finish
wait

echo "All requests sent"
