#!/bin/sh
rm -rf out
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out simulador.gui.App
