#!/bin/bash

rm -f jre11.tar.gz
wget --no-check-certificate "https://api.adoptopenjdk.net/v2/binary/releases/openjdk11?openjdk_impl=hotspot&os=linux&arch=x64&release=latest&type=jre" -O jre11.tar.gz

rm -Rf jre
mkdir jre

tar xf jre11.tar.gz --strip-components=1 --directory jre
