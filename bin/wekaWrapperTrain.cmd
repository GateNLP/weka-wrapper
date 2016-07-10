@ECHO OFF

SET ROOTDIR=%1
shift
SET arff=%1
shift
SET model=%1
shift
SET class=%1
shift

: create var with remaining arguments
set r=%1
:loop
shift
if [%1]==[] goto done
set r=%r% %1
goto loop
:done

SET java=java
if not [%JRE_HOME%]==[] SET java=%JRE_HOME%\bin\java.exe
if not [%JAVA_HOME%]==[] SET java=%JAVA_HOME%\bin\java.exe
: TODO not sure if forward or backward slashes and also, if we need quotes and where
: could be we need double quotes around everything except the star
%java% -cp %ROOTDIR%\target\*;%ROOTDIR%\target\dependency\* gate.lib.wekawrapper.WekaTraining %arff% %model% %class% %r%

