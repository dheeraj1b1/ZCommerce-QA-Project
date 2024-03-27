@echo off

set file=test-results.xml

REM Check for the existence of the file and remove if it exists
if exist "%file%" (
    del "%file%"
)

REM Copy the test results file
copy .\tmp\reports\tests\test\testng-results.xml %file%

REM Check and install lxml if not installed
REM Use pip3 if it exists, otherwise use pip
where pip3 >nul 2>&1
if %errorlevel%==0 (
    set PIP_CMD=pip3
) else (
    set PIP_CMD=pip
)

%PIP_CMD% show lxml >nul 2>&1
if %errorlevel% neq 0 (
    %PIP_CMD% install lxml
)

REM Run the Python script
REM Use python3 if it exists, otherwise use python
where python3 >nul 2>&1
if %errorlevel%==0 (
    set PYTHON_CMD=python3
) else (
    set PYTHON_CMD=python
)

%PYTHON_CMD% .\assessment\xmlassessment.py .\assessment\final_assessment_instructions.json %file%
