#!/bin/bash

# URL to which the request is made
url="http://localhost:6789/load"

# Generate random username and password
username="wrong"
password="wrong"

# Encode the credentials in base64 for the Authorization header
credentials=$(echo -n "$username:$password" | base64)

# Making a GET request using curl with the Authorization header and printing the response
echo "Sending GET request to $url with username: $username and password: $password"

response=$(curl -si -H "Connection: keep-alive" $url)

# Print the response
echo "Response from the server:"
echo "$response"
