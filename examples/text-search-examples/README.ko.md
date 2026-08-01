# text-search-examples

이 예제는 결정적인 입력을 청크 경계에서 나누고, eager `parseText`와 제한된 상태의 `AhoCorasickScanner` 결과를 비교합니다. 전역 UTF-16 offset을 출력하고 두 경로의 match가 같은지 검증합니다.

다음 명령으로 실행합니다.

```bash
./gradlew :examples:text-search-examples:run
```

테스트에서 경계를 가로지르는 키워드, 다수 match 출력, 잘못된 청크 크기를 검증합니다.
