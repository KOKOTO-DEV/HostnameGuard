# HostnameGuard

Minecraft Bukkit/Spigot/Paper 계열 서버에서 클라이언트가 접속할 때 사용한 hostname을 검사해서 허용한 도메인만 통과시키는 플러그인입니다.

## 변경점: 1.0.1

- 일부 Paper/Hybrid 서버에서 `api-version: '1.21'` 때문에 플러그인을 거부하는 경우를 피하기 위해 `plugin.yml`의 `api-version`을 제거했습니다.
- 기능상 Material/Item API를 사용하지 않으므로 api-version이 없어도 동작에 영향이 없습니다.

## 빌드

```bash
mvn clean package
```

결과물:

```text
target/HostnameGuard-1.0.1.jar
```

## 설치

1. `HostnameGuard-1.0.1.jar`를 서버의 `plugins/` 폴더에 넣습니다.
2. 서버를 재시작합니다.
3. `plugins/HostnameGuard/config.yml`을 서버별로 수정합니다.
4. `/hg reload` 또는 `/hostnameguard reload`로 설정을 다시 불러옵니다.

## 설정 예시

```yml
allowed-hosts:
  - "mafia.example.com"

allow-wildcards: false
wildcard-hosts: []

allow-unknown-host: false

kick-message: "&c공식 주소 &fmafia.example.com &c으로 접속해주세요."

log-denied: true
log-allowed: false
```

## 명령어

```text
/hg reload
/hg status
/hostnameguard reload
/hostnameguard status
```

## 권한

```text
hostnameguard.reload
hostnameguard.status
hostnameguard.bypass
```

`enable-bypass-permission`은 기본 false입니다. OP도 예외 없이 검사하고 싶으면 그대로 두면 됩니다.
