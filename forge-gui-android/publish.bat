cd ../forge-core
call mvn clean install
cd ../forge-ai
call mvn clean install
cd ../forge-game
call mvn clean install
cd ../forge-gui
call mvn clean install
cd ../forge-gui-mobile
call mvn clean install
cd ../forge-gui-android
call mvn -U -B clean -P android-release-build,android-release-sign,android-release-upload install -Dsign.keystore=lzy.jks -Dsign.alias=lzy -Dsign.storepass=123456 -Dsign.keypass=123456
pause