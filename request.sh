#!/bin/bash

# URL to which the request is made
url="http://localhost:6789/cgi/classes.cgi"

# Generate random username and password
username="wrong"
password="wrong"

# Encode the credentials in base64 for the Authorization header
credentials=$(echo -n "$username:$password" | base64)

# JSON data to send
jsonData='{"key1":"value1", "key2":"value2"}'

# Making a POST request using curl with the Authorization header and printing the response
echo "Sending POST request to $url with username: $username and password: $password"

response=$(curl -si -X POST -H "Authorization: Basic $credentials" -H "Content-Type: application/json" -d "$jsonData" $url)

# Print the response
echo "Response from the server:"
echo "$response"
