# 스냅숏 검증 라인

## 배경

이전 릴리스 후 snapshot validation은 저장소를 다음 개발 라인으로 다시 열고, 이에 맞는
upstream bluetape4k snapshot을 소비해야 했다.

## 결정

`baseVersion=0.1.3`을 설정하고, `snapshotVersion=`은 비워 두며,
`bluetape4k-bom:1.9.2-SNAPSHOT`을 소비한다.

## 결과

저장소는 `gradle.properties`에 snapshot suffix를 check-in하지 않고도
`publish-snapshot.yml`을 통해 `0.1.3-SNAPSHOT`을 publish할 수 있다.

## 검증

Snapshot validation train에서 대기 중.
