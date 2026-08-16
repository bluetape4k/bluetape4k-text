# tokenizer-safety-examples

토크나이저 모듈의 요청 경계, 버전이 있는 사전 reload, 일본어 backend 비교 경계를 실행하는 예제입니다.

## 일본어 backend 비교

`JapaneseBackendComparisonExamples`는 동일한 report shape으로
`Kuromoji IPADic`과 `Sudachi JVM`을 같은 corpus에서 실제 실행합니다. 두
backend 모두 첫 번째 broad POS field를 중립 observation으로 매핑하고,
Sudachi는 A/B/C split mode의 surface와 C mode의 POS를 함께 기록합니다.

report에는 두 backend의 license, runtime footprint, Gradle dependency 상태도
함께 출력합니다. Sudachi JVM은 `com.worksap.nlp:sudachi:0.8.0`을 중앙
catalog alias `bt4k.sudachi`로 사용합니다. Sudachi release는 `v0.8.*`이
`v1` 이전의 intermediate series라고 알리므로 정확한 버전을 고정했습니다.

선택한 dictionary는 공식 [SudachiDict `v20260428` release](https://github.com/WorksApplications/SudachiDict/releases/tag/v20260428)의
`core` archive입니다. archive는 72,238,136 bytes이고 SHA-256은
`40c8ffc095283f07aa06cae922e7b8147bf2919ec8830567b0b3f7a7efa3239f`이며,
압축 해제된 `system_core.dic`은 217,374,303 bytes입니다. Apache-2.0
license와 `LEGAL`/`LICENSE-2.0.txt` entry를 실행 시 검증합니다. 217 MB
binary는 저장소에 커밋하지 않고 `build/sudachi-dictionary/v20260428`에
다운로드·검증합니다.

이 예제는 정확도·latency benchmark가 아닙니다. 현재 비교 조건은 JDK 25,
동일한 세 입력(`選挙管理委員会`, `東京都へ行く`, `外国人参政権`), Kuromoji
IPADic bundled dictionary, SudachiDict core dictionary입니다. 예를 들어
`選挙管理委員会`는 Sudachi A/B/C에서 각각
`選挙/管理/委員/会`, `選挙/管理/委員会`, `選挙管理委員会`으로 나뉘며,
`外国人参政権`은 Kuromoji의 `外国/人参/政権`과 Sudachi의 mode별 결과가
다릅니다. 이 차이는 backend migration 시 surface/POS 계약을 별도로
검토해야 함을 보여 줍니다.

surface fixture의 근거는 [Sudachi 공식 split mode 문서](https://github.com/WorksApplications/Sudachi#the-modes-of-splitting)와
저장소의 [#105 평가 결과](../../docs/superpowers/research/2026-08-16-issue-105-japanese-backend-evaluation.md)입니다.
기존 #116 예제의 dependency-free report shape은 유지하고, 실제 외부
dependency/dictionary 경계만 이 후속 [Issue #284](https://github.com/bluetape4k/bluetape4k-text/issues/284)에서
확장했습니다.

다음 명령으로 비교를 포함한 예제를 실행합니다.

```bash
./gradlew :examples:tokenizer-safety-examples:prepareSudachiDictionary
./gradlew :examples:tokenizer-safety-examples:run
```

## 사전 reload 흐름

`DictionaryReloadExamples`는 작은 `blockwords-v1.txt` fixture를
`VersionedDictionary`에 로드하고, `blockwords-v2.txt`를 revision 2로 적용한
뒤 이전/이후 snapshot으로 금칙어 처리를 수행합니다. revision 3에서 loader
실패도 시도하며, 마지막으로 성공한 snapshot이 계속 활성 상태인지 확인합니다.

다음 명령으로 모듈을 실행합니다.

```bash
./gradlew :examples:tokenizer-safety-examples:run
```

테스트에서는 입력 길이 guard, 원문을 노출하지 않는 오류, processor 라우팅,
버전별 reload 결과, 실패한 reload 격리와 종단 간 moderation 응답을 검증합니다.

## Moderation service 흐름

`TextModerationService`는 입력 경계를 검증하고 지원 언어 구간을 감지한 뒤,
한국어·일본어 구간을 각 facade로 라우팅합니다. 하나의 Aho-Corasick
automaton으로 keyword/blockword를 찾고 마스킹하며, 응답에는 상태 코드,
감지 언어, 토큰 수, match 요약과 원문을 포함하지 않는 오류를 담습니다.
