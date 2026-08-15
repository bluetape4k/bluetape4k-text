# 변경 이력

`bluetape4k-text`의 주요 변경 사항을 이 문서에 기록한다.

형식은 [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)를 따른다.
이 프로젝트는 [Semantic Versioning](https://semver.org/spec/v2.0.0.html)을 따른다.

## [미배포]

### 추가

- Lingua 혼합 언어 예제가 detector를 pipeline 외부에서 한 번 생성해 여러 입력에 재사용하도록 보강됐다. preload와 lazy 모델 로딩 선택 기준과 동등한 결과를 검증하는 예제 테스트도 추가했다([#250](https://github.com/bluetape4k/bluetape4k-text/issues/250)).

### 변경

- #250 병합 이후의 완료 상태와 0.4.0 열린 이슈 대기열을 루트 `WIP.md`에 정렬했다([#273](https://github.com/bluetape4k/bluetape4k-text/issues/273)).

## [0.3.0] - 2026-08-06

### 변경

- 한국어 Keep a Changelog에서 `Fixed` 범주를 `버그 수정`으로 표준화했다([#225](https://github.com/bluetape4k/bluetape4k-text/issues/225)).
- GitHub Actions의 `Secret Scan (gitleaks)` 설치가 고정된 `v8.30.1` 릴리스 자산을 사용하도록 정비했다([#140](https://github.com/bluetape4k/bluetape4k-text/issues/140)).
- 현재 0.3.0 GitHub 이슈 대기열과 완료된 유지보수 작업을 루트의 WIP와 CHANGELOG에 반영했다([#227](https://github.com/bluetape4k/bluetape4k-text/issues/227)).

## [0.2.1] - 2026-06-26

### 추가

- 0.2.1 readiness milestone을 위해 실행 가능한 text-search, Lingua, tokenizer
  web-safety example을 추가했다([#109](https://github.com/bluetape4k/bluetape4k-text/issues/109), [#110](https://github.com/bluetape4k/bluetape4k-text/issues/110), [#111](https://github.com/bluetape4k/bluetape4k-text/issues/111)).
- builder test에서 Lingua low-accuracy mode, strict threshold behavior, detector
  reuse를 다뤘다([#98](https://github.com/bluetape4k/bluetape4k-text/issues/98)).
- root 및 module README 파일에 tokenizer와 blockword web-service input limit 및
  400/413 mapping을 문서화했다([#99](https://github.com/bluetape4k/bluetape4k-text/issues/99)).

### 변경

- 0.2.1 release line이 `io.github.bluetape4k:bluetape4k-bom:1.10.0`과 stable
  `catalog/2026-06-01-01` dependency catalog를 소비하도록 준비했다.
- release metadata가 library module만 publish하도록 runnable example project를
  `bluetape4k-text-bom` dependency constraint에서 제외했다.

## [0.2.0] - 2026-05-27

### 추가

- Korean/Japanese tokenizer가 dictionary data를 예측 가능하게 refresh할 수 있도록
  tokenizer dictionary update pipeline과 block-word governance를 정의했다([#85](https://github.com/bluetape4k/bluetape4k-text/issues/85)).
- tokenizer와 language-detection quality tracking을 위한 text quality benchmark
  plan, fixture corpus, README benchmark report를 publish했다([#83](https://github.com/bluetape4k/bluetape4k-text/issues/83), [#86](https://github.com/bluetape4k/bluetape4k-text/issues/86)).
- validation과 sanitized failure 검증을 위해 Korean/Japanese mixed-text tokenizer
  accuracy fixture와 security regression coverage를 추가했다([#84](https://github.com/bluetape4k/bluetape4k-text/issues/84), [#96](https://github.com/bluetape4k/bluetape4k-text/issues/96)).

### 변경

- stable release line을 열기 전에 PR validation에서 0.2.0 quality gate를 확립했다([PR #106](https://github.com/bluetape4k/bluetape4k-text/pull/106)).

## [0.1.2] - 2026-05-23

### 변경

- 0.1.2 release line을 위해 release catalog reference를 parameterize하고 catalog
  source에서 shared build alias를 resolve하도록 했다([PR #80](https://github.com/bluetape4k/bluetape4k-text/pull/80)).

## [0.1.1] - 2026-05-22

### 변경

- 0.1.1 release line이 `io.github.bluetape4k:bluetape4k-bom:1.9.0`을 소비하도록
  준비했다.

### 버그 수정

- `matchesAsFlow()`가 이제 trie traversal에서 default Aho-Corasick match를
  stream하고, collector가 `take(N)`으로 cancel하면 협력적으로 중단한다. overlap과
  word-boundary post-processing은 여전히 eager filtered path를 사용한다([#67](https://github.com/bluetape4k/bluetape4k-text/issues/67)).

## [0.1.0] - 2026-05-17

### 추가

- Root README hero image와 project-purpose, feature, language-switch entrypoint
  documentation을 갱신했다([PR #20](https://github.com/bluetape4k/bluetape4k-text/pull/20)).
- CI, nightly, snapshot, release, code-quality check를 위한 GitHub Actions
  workflow를 추가했다([PR #2](https://github.com/bluetape4k/bluetape4k-text/pull/2)).
- text library consumer를 위한 `bluetape4k-text-bom` BOM module을 추가했다([PR #7](https://github.com/bluetape4k/bluetape4k-text/pull/7)).
- text BOM module의 English/Korean README 파일을 추가했다([PR #8](https://github.com/bluetape4k/bluetape4k-text/pull/8)).

### 변경

- lessons, Kover, Dependabot, NMCP, compatibility guard, dependency maintenance를
  정비했다([PR #10](https://github.com/bluetape4k/bluetape4k-text/pull/10), [PR #11](https://github.com/bluetape4k/bluetape4k-text/pull/11), [PR #12](https://github.com/bluetape4k/bluetape4k-text/pull/12), [PR #16](https://github.com/bluetape4k/bluetape4k-text/pull/16), [PR #17](https://github.com/bluetape4k/bluetape4k-text/pull/17), [PR #18](https://github.com/bluetape4k/bluetape4k-text/pull/18), [PR #19](https://github.com/bluetape4k/bluetape4k-text/pull/19)).
- GitHub Actions와 annotation을 포함해 text dependency catalog와 dependency bump를
  갱신했다([PR #9](https://github.com/bluetape4k/bluetape4k-text/pull/9), [PR #14](https://github.com/bluetape4k/bluetape4k-text/pull/14), [PR #15](https://github.com/bluetape4k/bluetape4k-text/pull/15)).
- CI가 path filtering과 retry configuration을 사용하도록 했다([PR #5](https://github.com/bluetape4k/bluetape4k-text/pull/5)).
- Test code를 Kluent에서 `bluetape4k-assertions`로 이전했다([PR #6](https://github.com/bluetape4k/bluetape4k-text/pull/6)).
