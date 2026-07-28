# Dependencies Catalog 2026-05-26-01

## 배경

`bluetape4k-dependencies`는 중앙 관리 보안 dependency line을 담은
`catalog/2026-05-26-01`을 publish했다.

## 결정

Shared external library version을 local에서 pin하지 않고, downstream default
`bluetape4kDependenciesCatalogRef`를 새 catalog tag로 업데이트한다.

## 결과

저장소는 이제 기본적으로 `catalog/2026-05-26-01`에서 shared dependency version을 해석한다.

## 검증

`settings.gradle.kts`의 catalog ref를 확인했다.

## 향후 메모

Shared external library는 먼저 `bluetape4k-dependencies`를 업데이트하고 catalog를 tag한 뒤,
downstream repository를 해당 tag로 이동한다.
