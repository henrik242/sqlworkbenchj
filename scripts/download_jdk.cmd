@echo off
echo This batchfile downloads the most recent Java 11 JDK from https://adoptopenjdk.net/ 
echo and will then generate a Java runtime suitable for SQL Workbench/J
echo.
echo Downloading the JDK and creating the JRE will temporarily use about 600MB

set /P continue="Do you want to continue? (Y/N) "

if /I "%continue%"=="y" goto make_jre
if /I "%continue%"=="yes" goto make_jre

goto :eof

:make_jre
@powershell.exe -noprofile -executionpolicy bypass -file download_jdk.ps1

setlocal

for /D %%a in (%~dp0jdk*) do set jdkdir=%%~na
echo %jdkdir%

set jredir=%~dp0jre
set modules=java.base,java.desktop,java.datatransfer
set modules=%modules%,java.sql,java.sql.rowset
set modules=%modules%,java.xml,jdk.xml.dom
set modules=%modules%,java.net.http,jdk.net
set modules=%modules%,java.management
set modules=%modules%,jdk.unsupported,jdk.unsupported.desktop
set modules=%modules%,java.security.jgss
set modules=%modules%,jdk.charsets

rmdir /s /q %jredir% 2>nul

echo Generating JRE in %jredir%
%jdkdir%\bin\jlink --strip-debug --add-modules %modules% --output %jredir%

rem don't delete the files if something went wrong
if errorlevel 1 goto :eof

echo Removing JDK directory
rmdir /s /q %jdkdir% 2>nul

echo You can delete the downloaded ZIP archive now


