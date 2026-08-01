# bluetape4k-text Examples

[English](README.md) | 한국어

`bluetape4k-text` 모듈의 실행 가능한 예제입니다. 이 모듈들은 CI smoke test
용도이며 Maven Central에 publish하지 않습니다.

## 모듈

| 모듈 | 목적 |
|---|---|
| [`text-search-examples`](text-search-examples) | Aho-Corasick builder, streaming scanner, replacement, Flow 첫 알림 검색 |
| [`lingua-examples`](lingua-examples) | Lingua detector 재사용, 혼합 언어 구간화, 한국어·일본어 토크나이저 라우팅 |
| [`tokenizer-safety-examples`](tokenizer-safety-examples) | Tokenizer 요청 경계, 버전이 있는 사전 reload, 종단 간 moderation |

## 실행

```bash
./gradlew :examples:text-search-examples:run :examples:lingua-examples:run :examples:tokenizer-safety-examples:run
```
