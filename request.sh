#!/bin/bash

# URL to which the request is made
url="http://localhost:6789"

# Making a GET request using curl and printing the response
echo "Sending GET request to $url"
response=$(curl -s $url)

# Print the response
echo "Response from the server:"
echo "$response"
