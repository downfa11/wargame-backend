FROM mcr.microsoft.com/windows/servercore:ltsc2022

RUN powershell -Command \
    Invoke-WebRequest -Uri https://aka.ms/vs/16/release/vs_installer.exe -OutFile C:\vs_installer.exe; \
    Start-Process -FilePath C:\vs_installer.exe -ArgumentList '--quiet', '--wait', '--add', 'Microsoft.VisualStudio.Workload.VC', '--path', 'C:\BuildTools' -NoNewWindow -Wait; \
    Remove-Item -Force C:\vs_installer.exe

COPY ./wargame.vcxproj /app/wargame.vcxproj

COPY ./src /app/src

WORKDIR /app
RUN C:\BuildTools\MSBuild.exe wargame.vcxproj /p:Configuration=Release /p:Platform=x64

COPY ./bin/Release/wargame.exe /app/wargame.exe
CMD ["wargame.exe"]