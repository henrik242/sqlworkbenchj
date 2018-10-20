@echo off
echo Downloading the JDK and creating the JRE will temporarily use about 600MB

set /P continue="Do you want to continue? (Y/N) "

if /I "%continue%"=="y" goto make_jre
if /I "%continue%"=="yes" goto make_jre

goto :eof

:make_jre
@powershell.exe -noprofile -executionpolicy bypass -file download_jdk.ps1

for /D %%a in (%~dp0jdk*) do set jdkdir=%%~na
echo %jdkdir%

set jredir=%~dp0jre
setlocal
set modules=java.base,java.desktop,java.datatransfer
set modules=%modules%,java.sql,java.sql.rowset
set modules=%modules%,java.xml,jdk.xml.dom
set modules=%modules%,java.net.http,jdk.net
set modules=%modules%,jdk.zipfs
set modules=%modules%,java.management
set modules=%modules%,jdk.unsupported

rmdir /s /q %jredir% 2>nul

echo Generating JRE in %jredir%
%jdkdir%\bin\jlink --add-modules %modules% --output %jredir%

rem don't delete the files if something went wrong
if errorlevel 1 goto :eof

echo Removing JDK directory
rmdir /s /q %jdkdir% 2>nul

echo You can delete the downloaded ZIP archive now


