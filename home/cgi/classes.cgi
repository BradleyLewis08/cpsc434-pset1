#!/usr/bin/env python3

import os
import sys
import json
import cgi

def main():
    print("Content-Type: text/html\n")  # HTML is following, \n denotes the end of headers

    # Check if the request's content type is JSON
    if os.environ.get("CONTENT_TYPE", "") != "application/json":
        print_error("Invalid Content-Type. Expected application/json")
        return

    # Read the request body
    try:
        request_length = int(os.environ.get("CONTENT_LENGTH", 0))
    except ValueError:
        request_length = 0

    request_body = sys.stdin.read(request_length)

    # Parse the JSON data
    try:
        data = json.loads(request_body)
    except json.JSONDecodeError:
        print_error("Invalid JSON in request")
        return

    # Generate an HTML response
    print("<html>")
    print("<head>")
    print("<title>CGI Script Output</title>")
    print("</head>")
    print("<body>")
    print("<h1>Received JSON data:</h1>")
    print("<pre>{}</pre>".format(json.dumps(data, indent=2)))
    print("</body>")
    print("</html>")

def print_error(message):
    print("<html>")
    print("<head>")
    print("<title>Error</title>")
    print("</head>")
    print("<body>")
    print("<p>Error: {}</p>".format(message))
    print("</body>")
    print("</html>")

if __name__ == "__main__":
    main()
