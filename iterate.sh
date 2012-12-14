#!/bin/bash
set -o nounset
set -o errexit

mvn clean test

if [ -r data.7z ]; then
  rm -rf data/h2
  7z x data.7z
  echo '---done reloading old database---'
fi

mvn exec:java
