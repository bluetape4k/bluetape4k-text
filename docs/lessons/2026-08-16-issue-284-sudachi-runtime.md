# Issue #284 Sudachi dictionary runtime 경계

## 배경

Issue #116은 외부 사전이나 런타임 의존성 없이 일본어 backend 비교 예제를
제공했다. Issue #284에서는 Sudachi JVM을 실제로 실행해 Kuromoji IPADic과
같은 입력을 비교해야 했다. 예제 저장소에 사전 바이너리를 커밋하면 저장소
크기와 라이선스 추적 경계가 바뀌므로, 실행 시 build cache에 공식 사전을
준비하는 경계를 먼저 고정했다.

## 결정

- 런타임 의존성은 `com.worksap.nlp:sudachi:0.8.0`으로 고정한다. Sudachi
  `v0.8.0`은 공식 릴리스의 Apache-2.0 배포물이며, v1 이전의 중간 릴리스라는
  경고가 있으므로 버전을 동적으로 해석하지 않는다.
- 사전은 [SudachiDict v20260428 core release](https://github.com/WorksApplications/SudachiDict/releases/tag/v20260428)의
  `sudachi-dictionary-20260428-core.zip`만 사용한다. 다운로드 아카이브의
  크기는 `72,238,136` bytes, SHA-256은
  `40c8ffc095283f07aa06cae922e7b8147bf2919ec8830567b0b3f7a7efa3239f`이며,
  압축 해제한 `system_core.dic`의 크기는 `217,374,303` bytes다. 공식
  `LEGAL`과 `LICENSE-2.0.txt`도 함께 확인하지만 바이너리는 저장소에 넣지
  않는다. SudachiDict의 라이선스는 Apache-2.0이다.
- `prepareSudachiDictionary`가 공식 URL, 아카이브 크기, SHA-256, ZIP 항목,
  압축 해제 파일 크기를 확인하고 `build/sudachi-dictionary/v20260428`에
  저장한다. 테스트와 예제 실행은 이 task를 먼저 실행한다.
- 비교 corpus는 `選挙管理委員会`, `東京都へ行く`, `外国人参政権`으로
  고정한다. Kuromoji의 현재 결과와 Sudachi A/B/C 결과를 같은 JVM 실행에서
  수집하고, 예제의 공통 모델은 표면형과 broad POS만 보관한다. 세부 POS
  매핑은 후속 작업의 책임으로 남긴다.

## 결과

Sudachi A/B/C의 분할 결과는 입력 corpus와 사전 버전에 종속된다. 예를 들어
`選挙管理委員会`는 A에서 `選挙/管理/委員/会`, B에서
`選挙/管理/委員会`, C에서 하나의 표면형이 된다. `東京都へ行く`와
`外国人参政権`도 Kuromoji와 분할 경계가 다르므로, 이를 정확도 우위나
성능 우위로 해석하지 않는다.

## 검증

예제의 테스트는 두 backend가 `LIVE`/`MAPPED` 상태인지, 고정한 사전
버전·크기·SHA-256을 보고하는지, 세 corpus에서 알려진 A/B/C 표면형과
Kuromoji 결과가 일치하는지를 확인한다. 검증 조건은 현재 JVM과 위 core
사전이며, 정확도·지연 시간·메모리 footprint 벤치마크는 이 작업 범위에
포함하지 않는다.

## 놓친 점/놀람

Issue #116의 dependency-free sample 계약을 유지하면서 실제 Sudachi를
실행하려면, 외부 dependency와 사전 준비를 예제 Gradle 경계로 분리해야
했다. 사전 다운로드가 성공했다는 사실만으로 다른 JVM, 다른 corpus, 다른
사전 변형의 결과를 일반화할 수 없다.

## 향후 guard

Sudachi 버전이나 사전 버전을 바꿀 때는 `build.gradle.kts`의 dependency,
공식 release URL, 아카이브 SHA-256·크기, `system_core.dic` 크기, 예제
fixture를 함께 갱신하고 같은 corpus 테스트를 다시 실행한다. 정확도·지연
시간·footprint 주장을 추가하려면 corpus, JVM, 사전, 측정 방법을 먼저
고정한 별도 benchmark 이슈를 만든다. Issue #284 범위에서는 downstream
consumer build, publish, tag, release를 수행하지 않는다.

## 출처

- [Sudachi v0.8.0 release](https://github.com/WorksApplications/Sudachi/releases/tag/v0.8.0)
- [Sudachi README의 A/B/C 분할 예시](https://github.com/WorksApplications/Sudachi/blob/develop/README.md?plain=1)
- [SudachiDict v20260428 core release](https://github.com/WorksApplications/SudachiDict/releases/tag/v20260428)
