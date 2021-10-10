mvn -U -B clean -P windows-linux install > build.log 2>&1
copy /y forge-gui-desktop\target\*.bz2 .
pause