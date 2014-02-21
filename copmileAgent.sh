#!/bin/bash
#
# Usage
#   sh ./runServer.sh
#
javac -cp lib/adx-1.0.6.jar SimpleAdNetwork.java
jar cf lib/agent.jar *.class