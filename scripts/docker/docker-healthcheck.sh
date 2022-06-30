#!/bin/bash

set -e

COUNT=$(ps aux | grep "java -jar /app/cytomine-bioformats-wrapper.jar" | grep -v grep | wc -l)
if [ $COUNT -ge 1 ]; then
  # Healthy
  exit 0
fi

# Unhealthy
exit 1