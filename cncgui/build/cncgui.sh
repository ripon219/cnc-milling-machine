#!/bin/sh
# Run the CNC Milling Machine GUI Application
DIR="$(dirname "$0")"
java -jar $DIR/lib/cncgui.jar $* &

