#!/bin/bash

# Check if exactly one argument has been provided
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <config_file>"
    exit 1
fi

# Get the configuration file from the script arguments
config_file="$1"

# Check if the provided file exists
if [ ! -f "$config_file" ]; then
    echo "Error: Config file '$config_file' not found."
    exit 1
fi

find -name "*.java" > sources.txt
javac @sources.txt
java HttpServer -config "$config_file"
