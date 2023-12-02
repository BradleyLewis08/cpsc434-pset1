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

# Define the root of your source code and the directory for compiled classes
SRC_DIR="src"
BIN_DIR="bin"

# Create bin directory if it doesn't exist
mkdir -p "$BIN_DIR"

# Remove old compiled classes
find "$BIN_DIR" -type f -name "*.class" -delete

# Compile the Java files
find "$SRC_DIR" -name "*.java" > sources.txt
javac -d "$BIN_DIR" @sources.txt

# Run the server, specify the package name where your main class is located
# Assuming main class is in the 'server' package
java -cp "$BIN_DIR" server.HttpServer -config "$CONFIG_FILE"
