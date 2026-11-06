@echo off
setlocal
cd /d "%~dp0Python_Source"

if not exist ".venv\Scripts\python.exe" (
    echo Run setup_windows.bat first.
    pause
    exit /b 1
)

if not exist ".env" (
    echo Python_Source\.env is missing. Run setup_windows.bat first.
    pause
    exit /b 1
)

".venv\Scripts\python.exe" map_builder.py
if errorlevel 1 (
    echo Could not generate the map files. Check MySQL and .env settings.
    pause
    exit /b 1
)

echo Map preview and GeoJSON updated in Python_Source\output.
pause
