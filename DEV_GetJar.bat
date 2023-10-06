@echo off

set jarname=m3u8downloader-v3.1.1
set structure=com/vincentcodes/m3u8/*

:::: with Manifest
:: Include the libraries into the jar
:: cp -r lib/com/ .

cd classes
jar -cvfm %jarname%.jar Manifest.txt %structure%
mv %jarname%.jar ..

:: Remove files copied from "cp -r lib/com/ ."
:: rm -r ../com/

:::: without Manifest
:: cd classes
:: jar -cvf %jarname%.jar %structure%
:: mv %jarname%.jar ..

:: cd ../src
:: jar -cvf %jarname%-sources.jar %structure%
:: mv %jarname%-sources.jar ..

pause