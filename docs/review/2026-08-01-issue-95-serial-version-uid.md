# 공개 Serializable 모델 직렬화 버전 검토

## 적용 범위

공개 요청·응답 모델, 토크나이저 결과 모델, `CharArrayMap`, `CharArraySet`, `CharacterUtils`에 명시적인 `serialVersionUID`를 추가했다. 요청·응답 모델은 `AbstractMessage`를 상속하더라도 각 concrete 타입의 직렬화 계약을 고정하도록 별도 식별자를 둔다.

## 제외 기준

`KoreanPos`는 Java enum의 직렬화 규칙을 따르며 JVM이 enum 상수 이름으로 식별자를 관리하므로 사용자 정의 `serialVersionUID` 필드를 두지 않는다. `text-search`의 공개 Serializable 타입과 내부 타입은 이미 명시적인 식별자를 유지하고 있어 변경하지 않았다. #103에서 `internal`로 전환한 한국어 파서 상태 타입은 공개 모델 목록에서 제외했다.

## 호환성 및 운영

- 모든 식별자는 초기 공개 계약을 나타내는 `1L`로 고정했다.
- 필드 존재 여부와 primitive `long` 타입을 검사하는 반복 가능한 reflection 테스트를 core 및 Korean 모듈에 추가했다.
- 향후 공개 Serializable 모델을 추가할 때 해당 테스트 목록과 이 문서를 함께 갱신해야 한다.

## 검증

모델별 reflection 테스트와 모듈 컴파일·테스트·Detekt를 실행해 직렬화 필드와 기존 동작을 확인한다.
