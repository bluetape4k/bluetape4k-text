# Issue 22 Jackson3 Tokenizer 테스트

## 배경

`tokenizer-core` 테스트는 `bluetape4k-jackson2`와 Jackson2 Kotlin extension을
사용하고 있었다.

## 결정

Tokenizer 테스트를 `bluetape4k-jackson3`와 `tools.jackson` Kotlin extension으로
전환한다. 테스트 classpath에 필요한 최소 catalog alias도 추가한다.

## 결과

Tokenizer test serialization은 이제 Jackson3 기준으로 compile된다.

## 검증

- `./gradlew :tokenizer-core:testClasses`

## 향후 메모

작은 저장소에 Jackson3 alias를 추가할 때는 불완전한 transitive version metadata에
의존하지 말고, test-only direct import에 필요한 Jackson3 module alias도 명시적으로
추가한다.
