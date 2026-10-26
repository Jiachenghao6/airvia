# Airvia Privacy Policy

_Last updated: 26 October 2026_

Airvia is free and open source. It does not collect, store, or share your
personal data. There is no account, no advertising, no analytics, and no
crash reporting.

## What the app does with your data

- **Audio stays on your network.** Airvia captures audio from your phone
  (Android system audio, with your permission) and streams it directly to
  speakers on your own Wi-Fi network — your HomePod, Apple TV, or other
  AirPlay, Chromecast, or DLNA speakers. Your audio is never sent to the
  developer or to any third party.
- **Speaker discovery stays local.** Airvia finds speakers on your local
  network. Nothing about your network or your speakers leaves your home
  for discovery.
- **Pairing and settings stay on your device.** Speaker pairing
  credentials (for receivers that ask for a PIN), your preferences, and
  per-speaker settings are stored only on your phone. They are never
  uploaded anywhere.
- **Update check.** To tell you about new versions, the app asks GitHub's
  public releases list (`api.github.com`) which stable releases exist.
  GitHub sees that request the way any website sees a visit (including
  your IP address); the app sends no personal information with it.
  Pre-release test builds are never offered by the update checker.
- **Diagnostics stay with you.** Logs and diagnostics live on your
  device. They leave the device only if you choose to share them —
  for example, tapping Share on the Log tab when reporting a bug.

## Permissions

Airvia asks only for what streaming needs: network access and local
network discovery, audio capture, running the cast as a foreground
service, and notifications. Notification access is optional — it is only
used to recover the volume buttons if another app takes them over while
you are casting.

## Third-party software

Chromecast support uses Google's Cast SDK. The AirPlay implementation
was built with the help of open-source references (pyatv, owntone). No
third-party analytics or advertising SDK is included.

## Children

Airvia collects no data from anyone, including children.

## Changes

If this policy ever changes, the new version will be posted here in the
repository. Questions: open an issue at
https://github.com/jazzy1133/airvia/issues.
