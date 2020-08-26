cd ../forge-core
call mvn clean install
cd ../forge-ai
call mvn clean install
cd ../forge-game
call mvn clean install
cd ../forge-gui
call mvn clean install
cd ../forge-gui-desktop
call mvn -U -B clean -P windows-linux-release install
pause