# 릴리스 Catalog Guard

## 배경

AWS 0.3.0 release는 공유 release workflow 위험을 드러냈다. 오래된 GitHub repository
variable이 Gradle이 build script를 compile하기 전에 check-in된 `settings.gradle.kts`
catalog default를 override할 수 있었다.

## 결정

Stable tag release는 check-in된 catalog default를 사용한다. Manual dispatch는 명시적
`catalogRef` override를 사용할 수 있고, 그 다음 repository variable을 운영 대체 경로로
사용한다.

## 결과

Release workflow는 선택한 catalog source를 log로 남기고, Maven Central publish 전에
필수 catalog alias를 검증한다.

## 검증

`actionlint`를 실행하고, catalog selection branch를 local에서 검증하며, 현재 release
catalog가 필수 alias를 포함하는지 확인한다.

## 향후 지침

Repository catalog variable은 release train의 기준 소스가 아니라 manual release
override로 취급한다.
