#!/bin/sh
# Run the CNC Milling Machine GUI Application
DIR="$(dirname "$0")"
cd "$DIR"
java -Djava.library.path=lib/ -jar lib/cncgui.jar $* &
