#!/bin/bash

# Script to compile and run the Student Management System Demo

echo "========================================"
echo "Compiling Java files..."
echo "========================================"

# Compile both Java files
javac Student.java StudentDemo.java

# Check if compilation was successful
if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    echo ""
    echo "========================================"
    echo "Running StudentDemo..."
    echo "========================================"
    echo ""
    
    # Run the demo
    java StudentDemo
    
    # Clean up class files (optional)
    echo ""
    echo "========================================"
    echo "Cleaning up .class files..."
    echo "========================================"
    rm -f *.class
    echo "Done!"
else
    echo "Compilation failed! Please check for errors."
fi
