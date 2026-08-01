# lingua-examples

이 예제는 하나의 Lingua detector로 영어·한국어·일본어 혼합 텍스트를 구간으로 나눈 뒤, 한국어와 일본어 구간을 공개 토크나이저 facade로 전달합니다. 구간 offset은 원문 기준 UTF-16 인덱스입니다.

다음 명령으로 실행합니다.

```bash
./gradlew :examples:lingua-examples:run
```

테스트에서 구간 순서, 원문 offset, 언어별 토큰 수를 함께 검증합니다.
