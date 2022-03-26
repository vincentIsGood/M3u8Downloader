@echo off

set first=src/com/vincentcodes/m3u8/*.java
:: .java files are in encoding UTF-8
javac --release 11 -encoding UTF-8 -d classes -cp ./lib/*;./src/ %first%

pause