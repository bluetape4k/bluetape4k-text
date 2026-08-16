# 일본어 tokenizer backend 평가

날짜: 2026-08-16
이슈: [#105](https://github.com/bluetape4k/bluetape4k-text/issues/105)
대상 milestone: `0.4.0`

## 결정 요약

`0.4.0`에서는 현재 Kuromoji IPADic backend를 유지한다. 이번 평가에서
dependency, public API, dictionary를 변경하지 않는다.

향후 backend 확장이 필요하면 **Sudachi JVM을 optional backend 후보**로 먼저
검증한다. 다만 현재 공개 API가 Kuromoji의 `Token`과 `TokenBase`에 직접 결합되어
있으므로, Sudachi dependency를 바로 추가하지 않고 중립적인 token model과 backend
adapter 경계를 먼저 설계한다. 이 작업은 별도 이슈와 승인으로 진행한다.

Lindera는 일본어 품질 후보로는 확인했지만 Rust 중심 프로젝트라서 Kotlin/JVM
module의 직접 dependency 후보에서는 제외한다. JNI/FFI 또는 별도 프로세스 경계를
도입해야 하므로 이번 milestone의 migration cost와 맞지 않는다.

breaking migration은 선택하지 않는다. 현재 API의 source/ABI 계약과 품질 회귀를
동시에 바꾸며, Sudachi `v0.8.*` 자체도 `v1` 이전의 중간 release series로서 patch
release에서 breaking behavioral change가 생길 수 있다고 명시한다.

## 현재 구현 기준선

| 경계 | 현재 계약 | 근거 |
|---|---|---|
| dependency | `api(bt4k.kuromoji.ipadic)`, version catalog `kuromoji = "0.9.0"` | `tokenizer-japanese/build.gradle.kts:7-9`, `gradle/libs.versions.toml:9-10` |
| facade | `JapaneseProcessor.tokenize()`가 `List<com.atilika.kuromoji.ipadic.Token>`을 반환 | `tokenizer-japanese/src/main/kotlin/io/bluetape4k/tokenizer/japanese/JapaneseProcessor.kt:3,41-58` |
| tokenizer | lazy `com.atilika.kuromoji.ipadic.Tokenizer`를 재사용 | `tokenizer-japanese/src/main/kotlin/io/bluetape4k/tokenizer/japanese/tokenizer/JapaneseTokenizer.kt:3-4,8-40` |
| POS helper | `TokenBase.allFeaturesArray[0]`의 IPADic 품사 문자열에 의존 | `tokenizer-japanese/src/main/kotlin/io/bluetape4k/tokenizer/japanese/tokenizer/TokenBaseSupport.kt:3-17,31-101` |
| 사용자 문서 | Kuromoji IPADic token surface와 POS helper를 예제로 노출 | `tokenizer-japanese/README.md`, `tokenizer-japanese/README.ko.md`, 위 KDoc |

이 결합도 때문에 backend 교체는 dependency 한 줄을 바꾸는 작업이 아니다.
반환 타입, POS mapping, examples, tests, blockword processor의 token 사용 경계를
함께 검토해야 한다.

## 후보 비교

| 후보 | 유지보수 신호 | license | token quality/function | artifact·dictionary footprint | Kotlin/JVM 호환 | migration cost |
|---|---|---|---|---|---|---|
| Kuromoji IPADic `0.9.0` | repository는 archived가 아니지만 GitHub `pushed_at`이 2023-01-23이고 Maven jar의 `Last-Modified`가 2015-09-09이다. 현재 저장소가 이미 사용하는 안정된 기준선이다. | Apache-2.0 | IPADic 형태소와 현재 POS helper/test fixture가 이미 고정되어 있다. | `kuromoji-ipadic-0.9.0.jar` 13,343,016 bytes. IPADic dictionary가 artifact에 함께 들어간다. | 직접 JVM dependency이며 현재 API와 일치한다. | 현재 비용 0. 공개 `Token`/`TokenBase` 계약을 유지한다. |
| Sudachi JVM `0.8.0` | repository는 archived가 아니고 `v0.8.0`이 2026-05-26에 공개되었다. 다만 공식 release가 `v1` 이전 intermediate series이며 patch에도 breaking behavioral change가 가능하므로 exact pin이 필요하다. | Apache-2.0 (`LICENSE-2.0.txt`) | multiple-length segmentation, normalization, user dictionary를 제공한다. 공식 비교표에서 Kuromoji와 함께 accuracy/speed를 `Good`으로 제시하지만, 이 저장소의 corpus에 대한 동등한 수치는 아니다. `v0.7.0`에서 `List<Morpheme>`가 `MorphemeList`로 바뀐 ABI 영향도 있다. | `sudachi-0.8.0.jar` 231,049 bytes + runtime `jdartsclone` 22,059 bytes, `javax.json` 128,770 bytes. dictionary는 별도 배포되며 2026-07-24 release의 small/core/full이 각각 41,770,618 / 72,275,897 / 126,614,513 bytes다. | JVM artifact와 Gradle dependency로 통합할 수 있다. 현재 Kuromoji `Token`/IPADic POS를 Sudachi `Morpheme`/POS 체계로 바꾸는 adapter가 필요하다. | 높음. 중립 token model, POS mapping, dictionary 선택/configuration, surface/POS 비교 fixture, exact version pin이 필요하다. |
| Lindera | repository는 archived가 아니고 2026-08-16에 push되었다. | MIT | IPADIC, IPADIC NEologd, UniDic 및 사용자 dictionary를 지원하고 Rust benchmark를 제공한다. | 공식 문서는 Rust 구현을 기준으로 설명한다. Kotlin/JVM artifact가 아니라 Rust/Python/WASM binding 중심이다. | 직접 JVM dependency가 아니다. 이는 공식 문서가 Rust crate와 binding을 제시하고 이 저장소에 JVM bridge가 없다는 사실에 근거한 범위 판단이다. | 매우 높음. JNI/FFI 또는 별도 프로세스와 운영 경계를 새로 소유해야 하므로 이번 평가의 후보에서 제외한다. |

크기 수치는 동일한 packaging 단위를 의미하지 않는다. Kuromoji jar는 dictionary를
내장하고, Sudachi dictionary는 별도 asset이므로 jar 크기만으로 runtime footprint를
비교하지 않는다. Sudachi dictionary 크기는 선택한 `small/core/full`에 따라 달라진다.

## 품질과 호환성의 판단 경계

현재 `:tokenizer-japanese:test`는 Kuromoji API, POS helper, blockword processor,
dictionary lifecycle을 검증하는 회귀 게이트다. 2026-08-16에 다음 명령이
`97 passing`, `BUILD SUCCESSFUL`로 끝났다.

```text
./gradlew :tokenizer-japanese:test --no-daemon --max-workers=1
```

이 결과는 현재 backend의 회귀 안전성을 증명하지만 candidate 간 정확도나 속도를
증명하지 않는다. 이번 이슈에서는 비교 corpus, precision/recall, latency, heap
profile을 새로 만들지 않았다. 그런 수치가 필요하면 후보를 선택한 뒤 별도
benchmark/sample 이슈에서 동일 문장·동일 JVM·동일 dictionary 조건으로 측정해야
한다.

## migration 선택과 후속 경계

| 선택지 | 이번 결정 | 이유 |
|---|---|---|
| Kuromoji 유지 | **선택** | 0.4.0의 public API와 fixture를 보존하며 dependency 변경 없이 품질 회귀를 통제한다. |
| optional backend 추가 | **향후 후보로 보류** | Sudachi의 기능과 유지보수 신호는 유망하지만, 먼저 Kuromoji 독립 token model과 adapter 경계를 설계해야 한다. |
| breaking migration | **기각** | `Token`/`TokenBase`와 POS mapping을 바꾸는 범위가 크고, Sudachi의 pre-v1 behavioral stability 경고가 있다. |

Issue [#116](https://github.com/bluetape4k/bluetape4k-text/issues/116)은 이 결정에
따라 다음 단계에서 다룰 수 있다. 비교 sample은 현재 Kuromoji dependency를
대체하거나 새 dependency를 추가하지 않고, 승인된 candidate와 중립 model의
surface/POS mapping을 먼저 검증해야 한다.

## 근거 자료

- 현재 저장소: `tokenizer-japanese/build.gradle.kts`, `JapaneseProcessor.kt`,
  `JapaneseTokenizer.kt`, `TokenBaseSupport.kt`, `README.md`, `README.ko.md`.
- [Kuromoji 공식 repository](https://github.com/atilika/kuromoji)와
  [Kuromoji IPADic Maven metadata](https://central.sonatype.com/artifact/com.atilika.kuromoji/kuromoji-ipadic)
  — `0.9.0`, Apache-2.0, IPADIC artifact 계약.
- [Sudachi 공식 repository](https://github.com/WorksApplications/Sudachi) —
  multiple segmentation, normalization, user dictionary, JVM API와 공식 비교표.
- [Sudachi releases](https://github.com/WorksApplications/Sudachi/releases) —
  `v0.8.0` release 날짜, pre-v1 exact-pin 경고, `v0.7.0` API 변경 이력.
- [Sudachi Maven metadata](https://central.sonatype.com/artifact/com.worksap.nlp/sudachi)
  — `0.8.0`, Apache-2.0, runtime dependency metadata.
- [SudachiDict releases](https://github.com/WorksApplications/SudachiDict/releases)
  — `v20260723` dictionary asset 크기.
- [Lindera documentation](https://lindera.github.io/lindera/)와
  [Lindera repository](https://github.com/lindera/lindera) — Rust 구현, dictionary
  선택지, binding 범위, MIT license.

## 범위 밖 항목과 남은 위험

- 새 dependency, public API, dictionary asset, benchmark harness는 변경하지 않았다.
- downstream consumer build/publish, Maven Central, tag/release, workflow dispatch는
  Issue #105의 평가 범위가 아니므로 실행하지 않았다.
- license 표기는 각 프로젝트와 Maven metadata의 공개 정보에 근거한 기술 평가다.
  법률 검토나 dictionary 원문 license의 별도 법적 해석은 포함하지 않는다.
- Sudachi의 기능·품질 비교는 공식 기능 설명과 비교표를 요약한 것이며, 이 저장소의
  일본어 corpus에서 우월함을 의미하지 않는다.
- optional backend를 실제로 추가하려면 별도 승인된 Type-A/B 구현 계획과 API
  compatibility/benchmark DoD가 필요하다.
