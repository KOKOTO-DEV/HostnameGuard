# HostnameGuard

A plugin for Minecraft Bukkit/Spigot/Paper-based servers that checks the hostname used by clients when they connect and only allows approved domains.

## Changes: 1.0.1

- Removed `api-version` from `plugin.yml` to avoid cases where some Paper/Hybrid servers reject the plugin because of `api-version: '1.21'`.
- This does not affect functionality because the plugin does not use the Material/Item API.

## Build

```bash
mvn clean package
```

Output:

```text
target/HostnameGuard-1.0.1.jar
```

## Installation

1. Put `HostnameGuard-1.0.1.jar` into the server's `plugins/` folder.
2. Restart the server.
3. Edit `plugins/HostnameGuard/config.yml` for each server.
4. Reload the configuration with `/hg reload` or `/hostnameguard reload`.

## Configuration Example

```yml
allowed-hosts:
  - "mafia.example.com"

allow-wildcards: false
wildcard-hosts: []

allow-unknown-host: false

kick-message: "&cPlease connect using the official address: &fmafia.example.com"

log-denied: true
log-allowed: false
```

## Commands

```text
/hg reload
/hg status
/hostnameguard reload
/hostnameguard status
```

## Permissions

```text
hostnameguard.reload
hostnameguard.status
hostnameguard.bypass
```

`enable-bypass-permission` is `false` by default. Leave it as-is if you want even OPs to be checked without exceptions.
