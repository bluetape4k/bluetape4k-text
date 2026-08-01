# tokenizer-safety-examples

토크나이저 모듈의 요청 경계와 버전이 있는 사전 reload를 실행하는 예제입니다.

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
버전별 reload 결과와 실패한 reload 격리를 검증합니다.
