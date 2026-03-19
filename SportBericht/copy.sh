#!/bin/bash

cp /home/michael/git/SportBericht/SportBericht/target/SportBericht-6.0.war /home/michael/Programme/wildfly-35.0.0.Beta1/standalone/deployments

mvn install:install-file \
  -Dfile=/home/michael/git/SportBericht/SportBericht/target/SportBericht-6.0.war \
  -DgroupId=de.michael.ttbericht \
  -DartifactId=ttbericht \
  -Dversion=2.0.0 \
  -Dpackaging=war
