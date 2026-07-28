# Snapshot version parameterization

## 배경

Central Portal release는 `-SNAPSHOT`을 제거하기 위해 `gradle.properties`를 수정하도록
요구해서는 안 된다.

## 결정

`snapshotVersion=`은 기본적으로 비워 두고, `publish-snapshot.yml`이
`-PsnapshotVersion=-SNAPSHOT`을 전달하게 한다.

## 결과

`develop`은 release-ready 상태로 남고, snapshot publishing은 workflow command 안에서
명시적으로 수행된다.

## 검증

- `actionlint .github/workflows/publish-snapshot.yml`

## 향후 guard

`gradle.properties`의 기본값으로 `snapshotVersion=-SNAPSHOT`을 다시 도입하지 않는다.
