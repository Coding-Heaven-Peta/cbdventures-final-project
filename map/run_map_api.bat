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

".venv\Scripts\python.exe" -m flask --app map_api run --host 127.0.0.1 --port 5000 --no-reload
