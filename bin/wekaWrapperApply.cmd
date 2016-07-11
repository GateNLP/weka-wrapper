@ECHO OFF

SET ROOTDIR=%1
SET model=%2
SET header=%3

SET java=java
if not [%JRE_HOME%]==[] SET java=%JRE_HOME%\bin\java.exe
if not [%JAVA_HOME%]==[] SET java=%JAVA_HOME%\bin\java.exe
: TODO not sure if forward or backward slashes and also, if we need quotes and where
: could be we need double quotes around everything except the star
echo 1>&2 %java% -cp %ROOTDIR%/target/*;%ROOTDIR%/target/dependency/* gate.lib.wekawrapper.WekaApplication %model% %header%
%java% -cp %ROOTDIR%/target/*;%ROOTDIR%/target/dependency/* gate.lib.wekawrapper.WekaApplication %model% %header%
