# 2026-06-01 0.3.0 개발 시작

## 배경

`bluetape4k-text` `0.2.0`을 배포했고 `bluetape4k-dependencies` `1.2.0`에
포함했다.

## 결정

커밋된 `baseVersion`을 `0.3.0`으로 변경하되 `snapshotVersion=`은 비워 둔다.
이렇게 해야 릴리스 workflow가 snapshot 한정자를 명시적으로 주입할 수 있다.
직접 참조하는 `bluetape4k-bom` catalog 버전은 `1.11.0-SNAPSHOT`으로 맞춘다.

## 결과

다음 마이너 개발 라인을 시작할 준비가 되었다.

## 검증

- `gradle.properties`에 `baseVersion=0.3.0`이 설정되어 있다.
- `snapshotVersion=`은 비어 있다.
- `./gradlew help --no-daemon --console=plain`이 변경된 catalog을 해석한다.
