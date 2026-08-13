# ports/ — legacy island

The main mod now builds from a single **Stonecutter** source tree at the repository
root (see the root `README.md`). That toolchain covers Fabric 1.19+, Forge 1.18.2+,
and all NeoForge versions.

This directory retains **one** standalone project that the modern toolchain cannot
build, because NeoForged ModDevGradle's `legacyforge` variant does not reach that low:

- **`forge-1.15.2-1.16.5/`** — MinecraftForge 1.15.2–1.16.5 (the "legacy island").

It is a self-contained Gradle project. Build it directly:

```powershell
Set-Location .\ports\forge-1.15.2-1.16.5
.\gradlew.bat build
```

The old PowerShell port generator (`tools/generate-ports.ps1`) and the 15 generated
port folders were retired during the Stonecutter cutover; they remain recoverable
from git history if ever needed.
