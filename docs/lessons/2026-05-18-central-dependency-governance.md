# 중앙 Dependency Governance 동기화

## 배경

Downstream Dependabot PR이 공유 dependency version을 저장소별로 하나씩 올리면서
bluetape4k 조직 전반에 version drift가 생기고 있었다.

## 결정

공유 dependency version은 먼저 `bluetape4k-dependencies`에서 변경한 뒤
`sync-shared-versions.py`로 이 저장소에 반영한다. 이 저장소는 Dependabot에서 중앙
관리 dependency 이름도 ignore하여 향후 PR이 중앙 source of truth를 거치게 한다.

## 결과

Local version catalog와 `.github/dependabot.yml`은 이제 중앙 dependency-governance
policy를 따른다.

## 검증

- 이 저장소에서 `sync-shared-versions.py --write --check --summary`
- 이 저장소에서 `sync-dependabot-ignores.py --write --check --summary`
- `git diff --check`

## 향후 guard

중앙 관리 dependency에 대한 repo-local Dependabot PR을 merge하지 않는다.
`bluetape4k-dependencies`를 업데이트한 뒤 이 저장소에 sync한다.
