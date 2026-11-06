@echo off
setlocal
cd /d "%~dp0Python_Source"

where py >nul 2>nul
if errorlevel 1 (
    echo Python launcher "py" was not found. Install Python 3.10 or later.
    pause
    exit /b 1
)

if not exist ".venv\Scripts\python.exe" (
    py -3 -m venv .venv
    if errorlevel 1 (
        echo Could not create the virtual environment.
        pause
        exit /b 1
    )
)

".venv\Scripts\python.exe" -m pip install -r requirements.txt
if errorlevel 1 (
    echo Dependency installation failed.
    pause
    exit /b 1
)

if not exist ".env" (
    copy ".env.example" ".env" >nul
)

echo.
echo Enter your own SQLPro connection settings in Python_Source\.env.
echo Do not share that file or use a password posted in chat.
notepad ".env"
echo.
echo After saving .env, run run_map_api.bat from this handover folder.
pause
