# bt4k Version Catalog 사용

## 배경

`bluetape4k-text`에는 ecosystem catalog를 따라야 하는 local shared version pin이 있었다.

## 결정

공유 `bt4k` version catalog를 가져오고, shared leaf dependency constraint에는
`bt4kVersion(alias)`를 사용한다.

## 결과

선택한 shared dependency alias는 local version을 갖지 않으며, dependency management가
`bluetape4k-dependencies`에서 version을 공급한다.

## 검증

- `git diff --check`
- `./gradlew help --no-daemon --no-configuration-cache`
- `./gradlew compileKotlin --no-daemon --no-configuration-cache`

## 향후 지침

Text-specific library 선택은 local에 둘 수 있다. 그러나 이미 `bt4k`에 존재하는 shared
version value는 local에서 pin하지 않는다.
