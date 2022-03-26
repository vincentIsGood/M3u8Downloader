@echo off

:: _JAVA_OPTIONS only works on "java" command
set JAVAFX_HOME="C:\Program Files\Java\javafx-sdk-11.0.2\lib"
set JAVAFX_MODULES=javafx.base,javafx.controls,javafx.graphics,javafx.media

:: Do not add double quotes to the options "
:: set _JAVA_OPTIONS=--module-path=%JAVAFX_HOME% --add-modules=%JAVAFX_MODULES%

java -Dfile.encoding=UTF-8 -cp ./lib/*;./classes; com.vincentcodes.m3u8.Main

pause