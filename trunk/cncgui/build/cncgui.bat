@ECHO OFF
set JARPATH=%~dp0
cd "%JARPATH%"
start javaw -Djava.library.path=lib\ -jar lib\cncgui.jar 