# Text 0.2.0 릴리스 후 재개방

## 배경

0.2.0 release workflow는 tag `0.2.0`에서 성공했고, 저장소는 open 0.2.1 patch work와
함께 `develop`에서 개발을 계속한다.

## 결정

Stable release 후 `baseVersion`을 `0.2.1`로 올리고, shared bluetape4k release-line
policy에 맞춰 `snapshotVersion=`은 비워 둔다.

## 결과

Development branch는 publish된 0.2.0 release metadata를 바꾸지 않고 0.2.1 patch line으로
다시 열렸다.

## 검증

Reopen PR을 merge하기 전에 Gradle이 `version: 0.2.1`을 보고하는지, `snapshotVersion:`이
비어 있는지, PR CI가 통과하는지 확인한다.

## 향후 메모

Patch line이 명시적으로 닫히거나 연기되지 않았다면 post-release reopen에는 다음 open
patch milestone을 사용한다.
