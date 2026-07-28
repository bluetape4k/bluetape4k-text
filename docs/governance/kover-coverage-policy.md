# Kover Coverage 정책

## 현재 상태

`bluetape4k-text`는 Nightly에서 Kover XML report를 생성한다. `text-search`는
coverage 측정에서 benchmark package를 제외한다. 현재 어떤 모듈도 실패를
일으키는 coverage threshold를 강제하지 않는다.

## 정책

상태: report-only 전환 단계.

Tokenizer와 text-search 모듈은 동작 대부분이 결정적이므로 면밀한 coverage
monitoring 후보로 적합하다. 다만 focused issue가 gate를 명시적으로 다시
도입하기 전까지 coverage는 trend signal로 남겨야 한다.

## Threshold 계획

- Nightly artifact에서 tokenizer-core, tokenizer-korean, tokenizer-japanese,
  lingua, text-search coverage를 측정한다.
- Nightly XML report와 기존 coverage artifact upload를 사용해 coverage 회귀를
  찾는다.
- 모듈에 coverage 보강이 필요하면 focused issue를 연다. 기본 enforcement
  mechanism으로 실패 threshold를 도입하지 않는다.
- Benchmark package는 계속 제외한다.

## CI/Nightly 계약

Nightly는 Kover XML artifact를 upload하고 trend visibility를 유지한다. 향후 issue가
해당 gate를 명시적으로 다시 도입하지 않는 한, CI와 Nightly는 모듈이 고정 coverage
percentage보다 낮다는 이유만으로 실패해서는 안 된다.
