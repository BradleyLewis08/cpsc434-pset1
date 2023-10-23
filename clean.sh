#!/bin/bash

# Find all .class files and delete them
find . -type f -name "*.class" -exec rm -f {} \;
