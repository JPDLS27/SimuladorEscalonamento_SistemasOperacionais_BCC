@echo off
if exist out rmdir /s /q out
dir /s /b src\*.java > fontes.txt
javac -encoding UTF-8 -d out @fontes.txt
del fontes.txt
java -cp out simulador.gui.App
