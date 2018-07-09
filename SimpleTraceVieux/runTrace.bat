@echo off
echo Tracing %* with tools.jar...

java -cp "C:\Program Files\Java\jdk1.6.0_10\lib\tools.jar;." SimpleTrace %*

echo Finished.
