# Projects 1.9.2 BOM handoff

## 배경

`bluetape4k-projects` 1.9.2가 릴리스됐고, `bluetape4k-bom:1.9.2`는 Maven Central에서
확인된다.

## 결정

이 release-prep branch에서는 matching projects snapshot 대신 stable `bluetape4k-bom`
1.9.2 line을 사용한다.

## 결과

Version catalog는 이제 이 저장소의 자체 release line은 바꾸지 않은 채,
`io.github.bluetape4k:bluetape4k-bom`을 stable 1.9.2 release에서 해석한다.

## 검증

- `bluetape4k-bom:1.9.2`에 대한 Maven Central HTTP 200
- `./gradlew help --refresh-dependencies --no-daemon --no-configuration-cache --no-build-cache`
