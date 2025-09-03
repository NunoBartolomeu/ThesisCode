@echo off
SETLOCAL

REM Check argument
IF "%1"=="clean" (
   echo CLEAN START - data wipe + recompile

   REM Stop containers and remove all volumes
   docker compose down -v --remove-orphans

   REM Rebuild server & client images (dependencies cached if unchanged)
   docker compose build

   REM Start all services
   docker compose up -d
   echo Clean start complete!
) ELSE IF "%1"=="stop" (
   echo STOPPING CONTAINERS
   docker compose down
   echo Stop complete!
) ELSE (
   echo REGULAR START
   docker compose up -d
   echo Regular start complete!
)

REM Show running containers (unless stopping)
IF NOT "%1"=="stop" docker compose ps

ENDLOCAL
pause
