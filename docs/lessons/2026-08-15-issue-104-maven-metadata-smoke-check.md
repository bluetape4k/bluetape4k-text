# Maven Central metadata smoke check

## 배경

Issue #104의 릴리스 handoff에서는 프로젝트 버전, Text BOM, runtime
artifact가 같은 버전으로 Maven Central에 올라왔는지 확인해야 한다. publish
credential을 사용하는 release 동작과 artifact metadata를 읽는 smoke check는
서로 다른 상태 변경 경계다.

## 결정

`scripts/manual/maven_metadata_check.rb`를 release ref에서 실행한다. 이 검사는
`gradle.properties`의 프로젝트 버전을 기본 expected version으로 사용하고,
`--version`을 지정하면 현재 checkout과 별도로 확인할 published version을
선택한다.

검사 대상은 다음 6개 artifact다.

- `tokenizer-core`
- `tokenizer-japanese`
- `tokenizer-korean`
- `lingua`
- `text-search`
- `bluetape4k-text-bom`

각 artifact의 `maven-metadata.xml`을 GET으로 읽어 expected version이 포함되어
있는지 확인한다. 404 또는 네트워크 오류는 `missing`, metadata에 expected
version이 없으면 `stale`로 보고하고 비정상 종료한다.

## 사용법

release ref를 checkout한 뒤 기본 버전을 검사한다.

```bash
ruby scripts/manual/maven_metadata_check.rb
```

현재 checkout과 다른 이미 published된 release를 확인할 때는 버전을 명시한다.

```bash
ruby scripts/manual/maven_metadata_check.rb --version 0.3.0
```

이 명령은 Maven Central에 쓰지 않으며 credential도 요구하지 않는다. publish,
tag, release dispatch, downstream build를 대신하지 않는다.

## 검증

fixture 테스트는 전체 artifact 목록, 명시적 release version, missing metadata,
stale metadata를 검증한다.

```bash
ruby scripts/manual/maven_metadata_check_test.rb
```

## 향후 지침

release 문서 또는 downstream handoff를 갱신하기 전에 metadata smoke check를
먼저 실행하고, 실패한 artifact coordinate를 해결한 뒤 다음 단계를 진행한다.
