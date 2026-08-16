# tokenizer-safety-examples

토크나이저 모듈의 요청 경계, 버전이 있는 사전 reload, 일본어 backend 비교 경계를 실행하는 예제입니다.

## 일본어 backend 비교

`JapaneseBackendComparisonExamples`는 동일한 report shape으로 현재
`Kuromoji IPADic` 실행 결과와 `Sudachi JVM` 후보의 공식 split-mode surface
기록을 나란히 보여 줍니다. 현재 쪽은 `JapaneseProcessor`를 실제 호출하고
IPADic 첫 번째 POS field를 수집합니다. 후보 쪽은 `SudachiDict` system
dictionary를 저장소에 포함하지 않으므로 실행 결과가 아니라 `RECORDED`
fixture입니다.

report에는 두 backend의 license, runtime footprint, Gradle dependency 상태도
함께 출력합니다. 현재는 `bt4k.kuromoji.ipadic`만 사용하고 후보 dependency는
추가하지 않습니다.

후보의 `A/B/C` surface 차이는 migration 비용을 보여 주지만, 후보 POS는
`UNMAPPED`로 남겨 두었습니다. 따라서 이 예제는 정확도나 latency benchmark가
아니며, 실제 Sudachi dictionary-backed adapter와 POS mapping이 준비되기 전의
계약 검증입니다. 후보 runtime parity를 주장하려면 별도 dependency와
dictionary 승인, 동일 corpus 검증이 필요합니다.

surface fixture의 근거는 [Sudachi 공식 split mode 문서](https://github.com/WorksApplications/Sudachi#the-modes-of-splitting)와
저장소의 [#105 평가 결과](../../docs/superpowers/research/2026-08-16-issue-105-japanese-backend-evaluation.md)입니다.
실제 dictionary-backed 검증은 [후속 Issue #284](https://github.com/bluetape4k/bluetape4k-text/issues/284)에서 추적합니다.

다음 명령으로 비교를 포함한 예제를 실행합니다.

```bash
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
