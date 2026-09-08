# Issue #333 CI 의존 모듈 path-filter 회귀 방지

## 배경

`tokenizer-core` 변경은 `tokenizer-japanese`와 `tokenizer-korean`의 공개
의존성 입력을 바꾸므로 해당 소비자 테스트도 PR CI에서 실행되어야 한다. 기존
path-filter는 core 자체와 examples만 선택해 downstream 회귀를 놓칠 수 있었다.

## 결정 또는 발견

- 소비자 필터에는 직접 의존하는 모듈의 경로를 명시한다.
- Python validator는 Gradle shared path뿐 아니라 모듈 의존성 map과 Nightly의
  모듈 test job 존재도 검증한다.
- 회귀 테스트는 실제 workflow를 구조적으로 파싱하고, core-only 변경의 선택 결과와
  의존성 경로 제거 실패를 함께 확인한다. 기존 `ci-status` expected-job guard는
  그대로 유지한다.

## 결과

core-only 변경은 `tokenizer-core`, `tokenizer-japanese`, `tokenizer-korean`,
`examples`를 선택하고 `lingua`, `text-search`는 선택하지 않는다. Nightly는
모든 모듈 test job을 계속 포함하므로 PR 선택 실행과 전체 검증의 역할이 분리된다.

## 검증

- `PYTHONDONTWRITEBYTECODE=1 python3 .github/scripts/test_validate_ci_path_filters.py`
- `PYTHONDONTWRITEBYTECODE=1 python3 .github/scripts/validate-ci-path-filters.py`
- `python3 -m py_compile`은 pycache 생성 없이 별도 임시 디렉터리에서 실행한다.
- `actionlint .github/workflows/ci.yml`은 설치된 경우 실행한다.

## 향후 지침

새 모듈 또는 project dependency를 추가할 때는 CI path-filter, dependency map,
구조적 fixture, Nightly test job을 한 변경에서 함께 검증한다.

## 검증 범위

의존성 표는 PR의 production/test 모듈 테스트 선택을 위한 경로 정책이다. Gradle 전체 의존성 그래프나 benchmark 의존성의 자동 계산을 뜻하지 않는다. 특히 `text-search`의 tokenizer benchmark 의존성은 별도 benchmark 검증 범위이며 CI compile 작업의 제외 정책은 이번 변경에서 유지한다. production/test 의존성이 바뀌면 이 표와 경로 필터를 함께 수정해야 한다.

core-only 선택은 실제 workflow 내용을 읽는 로컬 fixture로 검증했다. 이 PR 자체는 `.github` 파일을 변경하므로 hosted CI에서 모든 관련 모듈이 선택되는 실행과 core-only fixture 결과를 구분해서 보고한다.
