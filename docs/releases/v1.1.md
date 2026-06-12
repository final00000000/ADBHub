# ADBHub v1.1

## Highlights

- Added device diagnostics for the empty-device state, including ADB path, ADB command query, device connection, authorization, offline, and unknown-device checks.
- Unified the "device required" prompt across Push APK, device commands, app management, file management, and log views.
- Hid command action controls until a device is connected.
- Reworked the device command page with vehicle-oriented quick actions, diagnostics, maintenance, and reboot groups.
- Added generic ADB device command execution with operation-log recording.
- Added Windows MSI and EXE native installer packaging for desktop releases.

## Verification

- `:desktop:compileKotlin`
- `:desktop:packageMsi`
- `:desktop:packageExe`

## Assets

- `ADBHub-1.1.0.msi`
  - SHA256: `338DED15597A41B674C47D3433F594EF51608C28BA95F8512FF5F1FA80E1E31D`
- `ADBHub-1.1.0.exe`
  - SHA256: `3443154423126E7932A9B015BD22F54851CA6B344D5B85D8A7F98A78EFED9733`
