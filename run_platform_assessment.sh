#!/bin/bash
file="test-results.xml"

# Check for the existence of the file and remove if it exists
if [ -f "$file" ] ; then
    rm "$file"
fi

# Copy the test results file
cp ./tmp/reports/tests/test/testng-results.xml $file

# Check and install lxml if not installed
# Use pip3 if it exists, otherwise use pip
if command -v pip3 >/dev/null 2>&1; then
    PIP_CMD="pip3"
else
    PIP_CMD="pip"
fi

$PIP_CMD show lxml >/dev/null 2>&1 || $PIP_CMD install lxml

# Run the Python script
# Use python3 if it exists, otherwise use python
if command -v python3 >/dev/null 2>&1; then
    PYTHON_CMD="python3"
else
    PYTHON_CMD="python"
fi

$PYTHON_CMD assessment/xmlassessment.py assessment/final_assessment_instructions.json $file
