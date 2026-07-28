# Text 0.2.1 Release Prep

## 배경

PR #150이 실행 가능한 example, web-service 안전 문서, Lingua coverage 작업을
닫은 뒤 0.2.1 milestone에는 open issue가 없다.

## 결정

`develop`에서 `bluetape4k-text` 0.2.1 patch stable release를 준비한다. Release-prep diff는
version metadata, release note, stable catalog default, 이 lesson으로 제한한다.

## 결과

Release metadata는 이제 `baseVersion=0.2.1`을 사용하고, `snapshotVersion=`은 비워 둔다.
Release workflow의 check-in된 catalog default는 `catalog/2026-06-01-01`을 가리키며,
local `bluetape4k-bom` reference는 Maven Central에서 보이는 `1.10.0` line을 사용한다. BOM
constraint는 실행 가능한 example project도 제외하므로 `bluetape4k-text-bom`은 stable library
module만 publish한다.

## 검증

Release를 tag하거나 dispatch하기 전에 local Gradle metadata, publication POM generation,
stale/SNAPSHOT POM 부재, local Maven publication, 현재 PR CI, 릴리스 준비 commit에 대한
fresh Nightly와 snapshot validation run을 검증한다.

## 향후 지침

Check-in된 release catalog, local version catalog, generated POM, release workflow input이
`-SNAPSHOT` dependency를 resolve할 수 있는 동안에는 stable release를 tag하거나 dispatch하지
않는다.
