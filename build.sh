#!/bin/bash
# Remove old maven artifacts
rm -r ~/.m2/repository/de/ukbonn/mwtek

# Build projects
cd ../utilities && mvn -U clean install &&
cd ../dashboardlogic && mvn -U clean install &&
cd ../dashboarddataprocessor && mvn -U clean install