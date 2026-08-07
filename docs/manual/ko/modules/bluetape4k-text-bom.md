# Text BOM

Text BOM은 런타임 라이브러리 다섯 개의 버전을 맞춘다. 의존성 제약만 담고 있으므로 BOM을 가져오는 것만으로는 토크나이저, 감지기, 검색 API가 classpath에 추가되지 않는다.

## 제공하는 기능

- `tokenizer-core`, `tokenizer-korean`, `tokenizer-japanese`, `lingua`, `text-search`의 버전 정렬
- Gradle platform과 Maven dependency management 지원
- 전체 `bluetape4k-dependencies` BOM이 맞지 않는 경우 사용할 Text 전용 BOM

## 의존성 추가하기

여러 bluetape4k 저장소의 라이브러리를 사용한다면 생태계 BOM을 우선한다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<release>"))
    implementation("io.github.bluetape4k.text:lingua")
    implementation("io.github.bluetape4k.text:text-search")
}
```

Text 모듈만 독립적으로 관리한다면 전용 BOM을 가져온다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k.text:bluetape4k-text-bom:0.3.0"))
    implementation("io.github.bluetape4k.text:tokenizer-korean")
}
```

## 동작 방식

BOM은 Gradle `java-platform` 프로젝트이며 배포 POM에 dependency-management 제약을 넣는다. 실행 클래스는 각 라이브러리 모듈에 있으므로 BOM만 가져온 빌드에서는 Text API를 호출할 수 없다.

## 선택 기준

일반 생태계 애플리케이션은 `bluetape4k-dependencies`를 선택한다. Text만 별도 릴리스 정책으로 관리해야 할 때 `bluetape4k-text-bom`을 사용한다. BOM을 가져온 뒤 개별 Text 모듈에 다시 버전을 붙이면 선택한 정렬 규칙을 우회하므로 피한다.

## 제약 사항

BOM은 호환 좌표를 정렬하지만 런타임 조합이 맞는지는 검사하지 않는다. `text-search` Flow 확장을 호출하는 애플리케이션이라면 선택적 coroutines 의존성도 직접 제공해야 한다.

## 다음 학습 경로

- [시작하기](../getting-started.md)
- [저장소 지도](../architecture/repository-map.md)
- [기능 선택](../guides/capability-selection.md)

## 소스 근거

- [BOM README](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/bom/README.md)
- [BOM 빌드](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/bom/build.gradle.kts)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `0.3.0` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### bom 아키텍처

[![bom 아키텍처](https://raw.githubusercontent.com/bluetape4k/bluetape4k-text/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/bom-architecture-01.png)](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/docs/images/readme-diagrams/bom-architecture-01.svg)

_배포본 README: [`bom/README.ko.md`](https://github.com/bluetape4k/bluetape4k-text/blob/aead213d2d25307d7d3684226943a5f95c7411f2/bom/README.ko.md)_

<!-- release-readme-diagrams:end -->
