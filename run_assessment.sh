#!/bin/bash
file="test-results.xml"

python3 assessment/xmlassessment.py assessment/final_assessment_instructions.json $file
