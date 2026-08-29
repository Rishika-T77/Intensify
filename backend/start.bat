@echo off
echo [Intensify] Loading environment and starting backend...

:: Load .env file
for /f "usebackq tokens=1,* delims==" %%A in (".env") do (
  if not "%%A"=="" if not "%%A:~0,1%"=="#" (
    set "%%A=%%B"
  )
)

:: Start Spring Boot
call mvnw.cmd spring-boot:run
