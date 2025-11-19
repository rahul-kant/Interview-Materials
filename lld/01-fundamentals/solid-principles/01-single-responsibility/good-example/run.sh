#!/bin/bash

# Script to compile and run the Good Example (follows SRP)

echo "========================================"
echo "Compiling all Java files..."
echo "========================================"

# Compile all Java files in the good-example directory
javac *.java

# Check if compilation was successful
if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    echo ""
    echo "========================================"
    echo "Running InvoiceDemo (Good Example)..."
    echo "========================================"
    echo ""
    
    # Run the demo
    java InvoiceDemo
    
    # Clean up class files
    echo ""
    echo "========================================"
    echo "Cleaning up .class files..."
    echo "========================================"
    rm -f *.class
    echo "Done!"
else
    echo "Compilation failed! Please check for errors."
fi
