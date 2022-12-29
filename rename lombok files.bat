@echo off

setlocal enabledelayedexpansion

for /f "delims=" %%f in ('dir /b /s /a-d "*.SCL.lombok"') do (
  set "file=%%f"
  set "newname=!file:.SCL.lombok=.class!"
  echo Copiando "!file!" a "!newname!"
  copy "!file!" "!newname!"
  echo Eliminando "!file!"
  del "!file!"
)

echo on

echo Extensiones de archivos cambiadas con éxito

endlocal
