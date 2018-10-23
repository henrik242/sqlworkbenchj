#!/bin/bash

rm -f jdk11.tar.gz
wget --no-check-certificate "https://api.adoptopenjdk.net/v2/binary/releases/openjdk11?openjdk_impl=hotspot&os=linux&arch=x64&release=latest&type=jdk" -O jdk11.tar.gz

# cleanup the temporary directory that holds the extracted JDK
rm -Rf _jdk11
mkdir _jdk11

# Unpack the JDK
tar xf jdk11.tar.gz --strip-components=2 --directory _jdk11

modules=java.base,java.desktop,java.datatransfer
modules=$modules,java.sql,java.sql.rowset
modules=$modules,java.xml,jdk.xml.dom
modules=$modules,java.net.http,jdk.net
modules=$modules,java.management
modules=$modules,jdk.unsupported,jdk.unsupported.desktop
modules=$modules,java.security.jgss
modules=$modules,jdk.charsets
# modules=$modules,jdk.localedata

rm -Rf jre
_jdk11/bin/jlink --add-modules $modules --output jre

rm -Rf _jdk11
rm -f jdk11.tar.gz
