# 회고: Lingua detector 재사용과 모델 로딩 선택 (#250)

**날짜:** 2026-08-15
**이슈:** #250
**모듈:** `examples/lingua-examples`

## 배경

혼합 언어 pipeline 예제가 호출할 때마다 detector를 만들고 있어, 애플리케이션이
detector 생명주기와 모델 로딩 방식을 선택하는 예를 보여 주지 못했다.

## 결정

- detector를 `createMixedLanguageDetector()`에서 한 번 생성하고
  `runMixedLanguagePipeline(text, detector)`에 주입한다.
- `preloadModels = true`는 첫 감지 전에 모델을 읽어 첫 사용 지연을 줄이는 대신
  초기화 작업과 메모리 사용량을 늘린다. `false`는 초기 비용을 줄이는 대신 첫
  감지 시 모델을 읽는다.
- 두 모드 모두 여러 입력에서 같은 detector API로 재사용하며, 호출마다 detector를
  새로 만들지 않는다.

## 검증

- 하나의 detector로 두 입력을 처리해 언어 구간과 토큰 수를 확인했다.
- preload와 lazy detector가 같은 segment와 token count를 반환하는지 확인했다.
- `:examples:lingua-examples:test`에서 6건이 통과했다.

## 후속 지침

새로운 Lingua 예제 pipeline은 detector를 호출 내부에서 생성하지 말고 애플리케이션
경계에서 구성해 주입한다. preload/lazy 선택을 문서화할 때는 측정하지 않은 지연 시간
수치를 제시하지 말고 초기화·메모리와 첫 사용 비용의 trade-off만 설명한다.
