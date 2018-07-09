@echo off
echo Compiling %1 with tools.jar...

javac -g -cp "C:\Program Files\Java\jdk1.6.0_10\lib\tools.jar;." %1

echo Finished.
