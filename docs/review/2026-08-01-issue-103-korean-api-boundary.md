# 한국어 토크나이저 API 경계 검토

## 검토 범위

이번 변경은 한국어 토크나이저의 공개 결과 모델과 내부 파서 상태를 분리한다. `KoreanProcessor`와 공개 모델의 식별자 및 파사드 시그니처는 유지하고, 파싱 과정에서만 사용하는 자료구조는 모듈 내부로 제한한다.

## 공개 타입

| 타입 | 공개 이유 | 직렬화 정책 |
|---|---|---|
| `KoreanToken` | 토큰화 결과 | 공개 `Serializable` 모델로 유지 |
| `KoreanChunk` | 청크화 결과 | 공개 `Serializable` 모델로 유지 |
| `Sentence` | 문장 분할 결과 | 공개 `Serializable` 모델로 유지 |
| `TokenizerProfile` | 토크나이저 설정 | 공개 `Serializable` 모델로 유지 |
| `KoreanPhrase` | 구문 추출 결과 | 공개 `Serializable` 모델로 유지 |
| `Hangul.HangulChar` | 한글 분해 결과 | 공개 `Serializable` 모델로 유지 |
| `Hangul.DoubleCoda` | 겹받침 값 | 공개 `Serializable` 모델로 유지 |
| `KoreanPos` | 품사 식별자 | 공개 enum으로 유지; Java enum의 직렬화 규칙 적용 |

## 내부 타입

`CandidateParse`, `PossibleTrie`, `ParsedChunk`, `KoreanPosTrie`, `PhraseBuffer`, `KoreanPosx`는 파서의 후보·트라이·구문 버퍼를 나타내는 구현 세부사항이다. 이 타입들은 `internal`로 제한하고 `Serializable` 구현을 제거했다. `NounTokenizer.koreanPosTrie`도 모듈 내부 접근만 허용한다. 공개 결과 모델이 내부 상태를 노출하지 않는지 Kotlin 컴파일러로 확인했다.

## 호환성 평가

- `KoreanProcessor`와 공개 결과 모델의 이름, 패키지, 파사드 반환 형태는 변경하지 않았다.
- 내부 타입은 이전에도 공개 문서나 파사드 계약의 일부가 아니었으므로 일반적인 공개 API 안정성 대상에서 제외한다.
- 내부 타입을 애플리케이션에서 직접 참조하던 소비자는 소스 및 바이너리 호환성 영향을 받을 수 있다. 그런 사용자는 공개 파사드와 결과 모델로 마이그레이션해야 한다.
- 공개 `Serializable` 모델의 `serialVersionUID` 보강은 후속 이슈 #95에서 일괄 처리한다.

## 검증

`tokenizer-korean` 주 소스와 테스트 소스를 함께 컴파일해 내부 타입의 공개 노출이 없는지 확인했다. 공개 API의 직렬화 식별자 검사는 #95의 반복 가능한 검사로 연결한다.
