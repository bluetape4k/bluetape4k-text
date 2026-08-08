# Projects 1.10.0 BOM 인계

## 배경

`bluetape4k-projects` 1.10.0을 배포했고 Maven Central에서
`bluetape4k-bom:1.10.0`을 확인할 수 있다.

## 결정

로컬 catalog의 projects BOM 버전을 1.9.2에서 1.10.0으로 변경하되,
이 저장소의 자체 릴리스 라인은 유지한다.

## 결과

이제 Text 빌드는 공유 bluetape4k 모듈 버전에 안정화된 projects 1.10.0 BOM을
사용한다.

## 검증

- Maven Central에서 `bluetape4k-bom:1.10.0` 요청이 HTTP 200을 반환한다.
