@echo off

set jarname=m3u8downloader-v4.1.0
set structure=com/vincentcodes/*

:::: with Manifest
:: Include the libraries into the jar
cp -r lib/com/ classes
cp -r lib/org/ classes

cd classes
jar -cvfm %jarname%.jar Manifest.txt %structure%
mv %jarname%.jar ..

:: Remove files copied from "cp -r lib/com/ ."
:: rm -r ../com/

:::: without Manifest
:: cd classes
:: jar -cvf %jarname%.jar %structure%
:: mv %jarname%.jar ..

cd ../src
jar -cvf %jarname%-sources.jar %structure%
mv %jarname%-sources.jar ..

pause