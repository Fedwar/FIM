@echo off
set /p seed=<seed.txt
java ^
	-classpath ^
		lib\*;out\production\classes^
	fleetmanagement.config.CommandsGenerator -v %seed% inscode-input.json
set /p DUMMY=Hit ENTER to continue...
