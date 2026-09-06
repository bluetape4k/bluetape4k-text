import io.bluetape4k.gradle.NormalizedSigningKeyId
import io.bluetape4k.gradle.normalizeSigningKeyId
import io.bluetape4k.gradle.resolveSigningKey
import io.bluetape4k.gradle.resolveSigningKeyId
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.plugins.signing.SigningExtension

/**
 * 빌드 설정의 project property 또는 환경 변수에서 값을 조회합니다.
 *
 * @param propertyKey 먼저 조회할 Gradle project property 키입니다.
 * @param envKey property가 비어 있을 때 조회할 환경 변수 키입니다.
 * @return property 또는 환경 변수 값입니다. 둘 다 없으면 빈 문자열입니다.
 */
fun Project.getEnvOrProperty(propertyKey: String, envKey: String): String =
    findProperty(propertyKey) as? String ?: System.getenv(envKey).orEmpty()

data class CentralPublishingConfig(
    val username: String,
    val password: String,
)

/**
 * 중앙 포털 자격증명을 Gradle project property 또는 환경 변수에서 로드합니다.
 *
 * property 키: `central.user`, `central.password`
 * 환경 변수 키: `CENTRAL_USERNAME`, `CENTRAL_PASSWORD`
 *
 * @return Central Portal 사용자 이름과 비밀번호를 담은 설정입니다.
 */
fun Project.resolveCentralPublishingConfig(): CentralPublishingConfig = CentralPublishingConfig(
    username = getEnvOrProperty("central.user", "CENTRAL_USERNAME")
        .ifBlank { getEnvOrProperty("centralPortalUsername", "CENTRAL_USERNAME") },
    password = getEnvOrProperty("central.password", "CENTRAL_PASSWORD")
        .ifBlank { getEnvOrProperty("centralPortalPassword", "CENTRAL_PASSWORD") },
)

data class SigningConfig(
    val keyId: String,
    val key: String,
    val password: String,
    val useGpgCmd: Boolean,
    val gpgExecutable: String,
    val gpgKeyName: String,
)

/**
 * 서명 설정을 Gradle project property 또는 환경 변수에서 로드합니다.
 *
 * 환경 변수 키: `SIGNING_KEY_ID`, `SIGNING_KEY`, `SIGNING_PASSWORD`
 *
 * @return PGP 또는 GPG 서명 설정입니다.
 */
fun Project.resolveSigningConfig(): SigningConfig {
    val normalizedKeyId: NormalizedSigningKeyId =
        normalizeSigningKeyId(getEnvOrProperty("signingKeyId", "SIGNING_KEY_ID"))
    val keyId = resolveSigningKeyId(normalizedKeyId.value)
    normalizedKeyId.warning?.let(project.logger::warn)
    val key = resolveSigningKey(getEnvOrProperty("signingKey", "SIGNING_KEY"))
    val password = getEnvOrProperty("signingPassword", "SIGNING_PASSWORD")
    val useGpgCmd = getEnvOrProperty("signingUseGpgCmd", "SIGNING_USE_GPG_CMD").toBoolean()
    val gpgExecutable = getEnvOrProperty("signing.gnupg.executable", "GPG_EXECUTABLE")
        .ifBlank { "/opt/homebrew/bin/gpg" }
    val gpgKeyName = getEnvOrProperty("signing.gnupg.keyName", "GPG_KEY_NAME").ifBlank { keyId }
    return SigningConfig(keyId, key, password, useGpgCmd, gpgExecutable, gpgKeyName)
}

 /**
 * 배포 publication의 Maven 서명을 설정합니다.
 * - CI: `SIGNING_KEY` + `SIGNING_PASSWORD` 환경 변수로 인메모리 PGP 서명
 * - 로컬: `signingUseGpgCmd=true` 또는 gpg-cmd 설정으로 서명
 *
 * @param publicationName 서명할 Maven publication 이름입니다.
 */
fun Project.configurePublishingSigning(publicationName: String) {
    val config = resolveSigningConfig()
    extensions.configure<SigningExtension> {
        when {
            config.key.isNotBlank() && config.password.isNotBlank() -> {
                useInMemoryPgpKeys(config.keyId.ifBlank { null }, config.key, config.password)
                val publishing = project.extensions.findByType(PublishingExtension::class.java)
                publishing?.publications?.findByName(publicationName)?.let { sign(it) }
            }
            config.useGpgCmd -> {
                if (file(config.gpgExecutable).exists()) {
                    project.extensions.extraProperties["signing.gnupg.executable"] = config.gpgExecutable
                }
                if (config.gpgKeyName.isNotBlank()) {
                    project.extensions.extraProperties["signing.gnupg.keyName"] = config.gpgKeyName
                }
                useGpgCmd()
                val publishing = project.extensions.findByType(PublishingExtension::class.java)
                publishing?.publications?.findByName(publicationName)?.let { sign(it) }
            }
            else -> {
                // 서명 키 없음 — 로컬 개발 빌드에서는 서명 건너뜀
            }
        }
    }
}
