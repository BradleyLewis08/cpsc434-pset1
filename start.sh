#!/bin/bash

# Check if an argument has been provided
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <config-file>"
    exit 1
fi

# Your configuration file from the command line arguments
CONFIG_FILE=$1

# Ensure the file exists
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Config file '$CONFIG_FILE' not found."
    exit 1
fi

# Find all Java files and compile them
find . -name "*.java" > sources.txt
javac @sources.txt

# Assuming your class files are in a directory structure that matches your package structure,
# 'java' needs the package name to find the main class.
# Also, ensure to provide the correct path for your config file if your current directory changes.
java HttpServer -config $CONFIG_FILE
