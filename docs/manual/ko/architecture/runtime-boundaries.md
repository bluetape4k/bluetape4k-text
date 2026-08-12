# 런타임 경계

같은 저장소에 있어도 언어 모델, 토크나이저 사전, 검색 automaton은 초기화와 메모리, 동시성 특성이 다르다. 각각의 수명을 설계 요소로 다뤄야 한다.

## 언어 감지기

Lingua 감지기는 언어 모델을 읽는다. 미리 로드하면 시작할 때 시간과 메모리를 쓰는 대신 이후 호출이 예측하기 쉬워지고, 지연 로딩을 택하면 초기 비용을 줄이는 대신 첫 호출로 비용이 이동한다. 어느 쪽이든 지원 언어 집합에 맞춰 감지기를 한 번 만들고 요청 사이에서 재사용한다.

모든 언어 모델을 요청마다 새로 구성하는 방식은 가장 넓은 모델 집합과 가장 많은 할당을 동시에 선택하는 셈이다. 실제 지원 언어만 넣고 배포 환경에서 시작 시간을 측정하자.

## 한국어·일본어 프로세서

`KoreanProcessor`와 `JapaneseProcessor`는 공유 facade 객체다. 한국어 API는 동시 호출에 안전하다. 두 facade는 startup/readiness에서 호출할 수 있는 suspend `preload()`를 제공하며 provider의 단일 초기화 lifecycle을 공유한다. 직접 동기 조회는 호환성 fallback으로 유지되므로 첫 조회에서 호출 스레드를 차단할 수 있다.

초기 응답 시간도 일정해야 한다면 readiness 전에 해당 facade의 `preload()`를 호출하고 cold/warm 시간을 기록한다. 런타임 사전을 변경하면 프로세스 전체의 이후 결과가 달라지므로 관리되지 않은 요청에서 변경 API를 호출하지 않는다.

## 사전 로딩

`DictionaryProvider.readWords`는 여러 리소스를 coroutine 기반 비동기 방식으로 합친다. processor facade의 `preload()`를 애플리케이션 초기화 coroutine에서 호출하고 결과 `CharArraySet`을 재사용하자. 리소스는 일반 텍스트나 gzip 형식을 사용할 수 있다.

클러스터 전체에 정책 업데이트를 배포하는 일은 loader의 책임이 아니다. 인스턴스가 같은 정책을 써야 한다면 애플리케이션 사전을 별도로 버전 관리하고 요청 경로 밖에서 설치한다.

## 검색 automaton

`AhoCorasickAutomaton.Builder`는 키워드와 옵션을 구성하는 동안만 가변이다. `build()`가 반환한 automaton은 불변이라 공유하기 좋다. 대용량 키워드 집합을 요청마다 다시 만들면 trie와 failure link 구성 비용을 계속 반복하게 된다.

`matchesAsFlow`는 `Dispatchers.Default`를 사용하는 Flow 경로다. `take(1)` 같은 제한 수집을 지원하지만 CPU 검색 비용이 없어지는 것은 아니다. 즉시 메모리 결과가 필요하면 일반 메서드를, 주변 처리 흐름이 수집·취소 의미를 필요로 하면 Flow를 사용한다.

## 권장 수명

| 자원 | 권장 수명 | 피해야 할 방식 |
|---|---|---|
| 설정된 `LanguageDetector` | 언어 정책별 애플리케이션 singleton | 요청마다 생성 |
| 프로세서 facade | 공유 객체 | 숨은 가변 정책을 가진 wrapper |
| 애플리케이션 사전 | 버전이 있는 초기화 상태 | 요청마다 디스크·네트워크에서 다시 읽기 |
| 빌드된 Aho-Corasick automaton | 공유 불변 snapshot | 요청마다 다시 빌드 |
| 요청 모델 | 승인된 요청마다 하나 | 오류 로그에 제출 본문 저장 |

## 준비와 관측

실제로 쓸 경로만 warmup하고 결과를 측정하자. 감지기 구성 시간, 첫 토큰화 시간, 사전 크기, automaton 키워드 수, 입력 길이, 일치 수, 정제된 오류 분류가 유용하다. 사용자 원문을 일반 오류 지표의 label로 넣어서는 안 된다.

[시작과 메모리](../operations/startup-and-memory.md)에서 배포 체크리스트를, [입력 안전성](../guides/input-safety.md)에서 요청 경계 규칙을 확인할 수 있다.

## 소스 근거

- [Lingua 모듈 계약](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/lingua/README.md)
- [일본어 프로세서 계약](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-japanese/README.md)
- [Text search Flow 확장](https://github.com/bluetape4k/bluetape4k-text/tree/0.3.0/text-search/src/main/kotlin/io/bluetape4k/text/search/flow)
