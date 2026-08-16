# lingua-examples

이 예제는 재사용할 Lingua detector 하나로 영어·한국어·일본어 혼합 텍스트를 구간으로 나눈 뒤, 한국어와 일본어 구간을 공개 토크나이저 facade로 전달합니다. 구간 offset은 원문 기준 UTF-16 인덱스입니다. 작은 offline corpus quality sample도 같은 실행 모듈에서 제공합니다.

애플리케이션 경계에서 detector를 한 번 만들고 각 pipeline 호출에 전달합니다.

```kotlin
val detector = createMixedLanguageDetector(preloadModels = true)
val result = runMixedLanguagePipeline("Hello 안녕하세요 こんにちは", detector)
```

`preloadModels = true`는 선택한 언어 모델을 초기화할 때 모두 읽으므로 첫 감지 비용을 줄이는 대신 시작 작업과 메모리 사용량이 늘어납니다. 시작 비용이 더 중요하고 지연 로딩을 허용할 수 있다면 `false`를 사용합니다. 두 모드 모두 여러 입력에서 재사용할 수 있으며, 호출마다 detector를 새로 만들지 않습니다.

다음 명령으로 실행합니다.

```bash
./gradlew :examples:lingua-examples:run
```

실행 결과의 `Offline corpus quality sample` 표는 체크인된 작은 fixture의 감지 언어와 한국어·일본어 token count를 보여 줍니다. 이 표는 결정적인 consumer smoke check이며 외부 corpus의 precision, recall, F1이나 성능 benchmark가 아닙니다.

private corpus로 교체하려면 같은 `id<TAB>LANGUAGE[,LANGUAGE...]<TAB>text` 형식의 UTF-8 파일을 만들고 다음처럼 경로를 지정합니다.

```bash
./gradlew :examples:lingua-examples:run \
  -Dbluetape4k.offline-corpus.path=/path/to/private-corpus.tsv
```

테스트에서 detector 재사용, preload/lazy 결과 동등성, 구간 순서, 원문 offset, 언어별 토큰 수, offline fixture와 private corpus 경계를 함께 검증합니다.
