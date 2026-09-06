package io.bluetape4k.text.search

import io.bluetape4k.support.requireInRange
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty
import java.io.Serializable
import java.util.Collections

/**
 * 원문 UTF-16 code unit을 가리키는 검증된 half-open 범위입니다.
 *
 * [startInclusive]는 포함되고 [endExclusive]는 포함되지 않습니다. 정규화된 문자열의 offset이
 * 아니라 redaction 대상 원문 문자열의 offset을 저장하므로 surrogate pair도 원문 길이와 함께
 * 보존할 수 있습니다.
 */
@ConsistentCopyVisibility
data class RedactionRange private constructor(
    val startInclusive: Int,
    val endExclusive: Int,
) : Serializable {

    /** 범위의 UTF-16 code-unit 길이입니다. */
    val length: Int
        get() = endExclusive - startInclusive

    override fun toString(): String =
        "RedactionRange(start=$startInclusive, end=$endExclusive, length=$length)"

    companion object {
        private const val serialVersionUID: Long = 1L

        /** 검증된 half-open 원문 범위를 생성합니다. */
        @JvmStatic
        fun of(startInclusive: Int, endExclusive: Int): RedactionRange {
            startInclusive.requireInRange(0, Int.MAX_VALUE, "startInclusive")
            require(endExclusive > startInclusive) { "endExclusive must be greater than startInclusive" }
            return RedactionRange(startInclusive, endExclusive)
        }
    }
}

/**
 * [TextRedactor]가 사용하는 keyword 또는 regular-expression 규칙입니다.
 *
 * 규칙 식별자와 category만 metadata로 노출하며 keyword와 regex 원문은 저장된 결과나
 * [toString]에 포함하지 않습니다. regex는 신뢰된 규칙을 대상으로 실행해야 하며, 이 API는
 * 일반적인 ReDoS 방지 보장을 제공하지 않습니다.
 */
class RedactionRule private constructor(
    val id: String,
    val category: String,
    val priority: Int,
    internal val kind: RedactionRuleKind,
    internal val keyword: String?,
    internal val regex: Regex?,
) : Serializable {

    override fun toString(): String =
        "RedactionRule(id=$id, category=$category, priority=$priority, kind=$kind)"

    companion object {
        private const val serialVersionUID: Long = 1L
        private const val MIN_PRIORITY = 1
        private const val MAX_PRIORITY = 1_000
        private const val MAX_KEYWORD_LENGTH = 1_024
        private const val MAX_PATTERN_LENGTH = 4_096

        /** keyword를 사용하는 redaction 규칙을 생성합니다. */
        @JvmStatic
        fun keyword(
            id: String,
            category: String,
            keyword: String,
            priority: Int = 50,
        ): RedactionRule {
            val value = keyword.trim()
            value.requireNotBlank("keyword")
            value.length.requireInRange(1, MAX_KEYWORD_LENGTH, "keyword.length")
            return create(
                id = id,
                category = category,
                priority = priority,
                kind = RedactionRuleKind.KEYWORD,
                keyword = value,
                regex = null,
            )
        }

        /** regular-expression redaction 규칙을 생성합니다. */
        @JvmStatic
        fun regex(
            id: String,
            category: String,
            patternSource: String,
            priority: Int = 50,
        ): RedactionRule {
            val source = patternSource.trim()
            source.requireNotBlank("patternSource")
            source.length.requireInRange(1, MAX_PATTERN_LENGTH, "patternSource.length")
            val compiled = try {
                Regex(source)
            } catch (_: IllegalArgumentException) {
                // PatternSyntaxException이 민감한 pattern 원문을 예외 메시지에 포함할 수 있으므로
                // caller에게는 안전한 고정 메시지만 노출합니다.
                throw IllegalArgumentException("Invalid regular-expression pattern")
            }
            return create(
                id = id,
                category = category,
                priority = priority,
                kind = RedactionRuleKind.REGEX,
                keyword = null,
                regex = compiled,
            )
        }

        private fun create(
            id: String,
            category: String,
            priority: Int,
            kind: RedactionRuleKind,
            keyword: String?,
            regex: Regex?,
        ): RedactionRule {
            val safeId = id.trim()
            val safeCategory = category.trim()
            validateMetadataSlug(safeId, "id")
            validateMetadataSlug(safeCategory, "category")
            priority.requireInRange(MIN_PRIORITY, MAX_PRIORITY, "priority")
            return RedactionRule(
                id = safeId,
                category = safeCategory,
                priority = priority,
                kind = kind,
                keyword = keyword,
                regex = regex,
            )
        }

        private fun validateMetadataSlug(value: String, parameterName: String) {
            require(SAFE_METADATA_SLUG.matches(value)) {
                "$parameterName must be a lowercase metadata slug"
            }
        }
    }
}

/**
 * redaction 실행에 필요한 불변 정책 snapshot입니다.
 *
 * [rules]는 방어적으로 복사됩니다. keyword는 기본적으로 대소문자를 무시하고 NFC로 정규화하며,
 * 필요하면 [keywordIgnoreCase]와 [keywordNormalization]을 명시할 수 있습니다. [maxTextLength]와
 * [MAX_RULE_COUNT]는 caller 입력과 정규식 실행 비용을 제한하는 상한이며, regex engine의
 * 일반적인 실행 시간 보장을 대신하지 않습니다.
 */
class RedactionPolicy private constructor(
    val rules: List<RedactionRule>,
    val maskChar: Char,
    val maxTextLength: Int,
    val keywordIgnoreCase: Boolean,
    val keywordNormalization: NormalizationForm,
) : Serializable {

    override fun toString(): String =
        "RedactionPolicy(ruleCount=${rules.size}, maskChar=<redacted>, maxTextLength=$maxTextLength, " +
            "keywordIgnoreCase=$keywordIgnoreCase, keywordNormalization=$keywordNormalization)"

    companion object {
        private const val serialVersionUID: Long = 1L
        const val DEFAULT_MAX_TEXT_LENGTH: Int = 4_096
        const val MAX_RULE_COUNT: Int = 128

        /** caller collection을 snapshot으로 복사한 immutable 정책을 생성합니다. */
        @JvmStatic
        @JvmOverloads
        fun of(
            rules: Collection<RedactionRule>,
            maskChar: Char = '*',
            maxTextLength: Int = DEFAULT_MAX_TEXT_LENGTH,
            keywordIgnoreCase: Boolean = true,
            keywordNormalization: NormalizationForm = NormalizationForm.NFC,
        ): RedactionPolicy {
            rules.requireNotEmpty("rules")
            rules.size.requireInRange(1, MAX_RULE_COUNT, "rules.size")
            require(!maskChar.isWhitespace() && !maskChar.isSurrogate()) {
                "maskChar must be a visible non-surrogate character"
            }
            maxTextLength.requireInRange(1, Int.MAX_VALUE, "maxTextLength")

            val snapshot = rules.toList()
            require(snapshot.map { it.id }.toSet().size == snapshot.size) {
                "rules must have unique ids"
            }
            return RedactionPolicy(
                rules = Collections.unmodifiableList(snapshot),
                maskChar = maskChar,
                maxTextLength = maxTextLength,
                keywordIgnoreCase = keywordIgnoreCase,
                keywordNormalization = keywordNormalization,
            )
        }
    }
}

/** redaction된 원문 범위와 안전한 rule metadata입니다. */
@ConsistentCopyVisibility
data class RedactionSpan private constructor(
    val range: RedactionRange,
    val category: String,
    val ruleIds: List<String>,
    val matchedLength: Int,
) : Serializable {

    override fun toString(): String =
        "RedactionSpan(range=$range, category=$category, ruleIds=$ruleIds, matchedLength=$matchedLength)"

    companion object {
        private const val serialVersionUID: Long = 1L

        internal fun of(
            range: RedactionRange,
            category: String,
            ruleIds: List<String>,
        ): RedactionSpan {
            require(category.isNotBlank()) { "category must not be blank" }
            require(ruleIds.isNotEmpty()) { "ruleIds must not be empty" }
            return RedactionSpan(
                range = range,
                category = category,
                ruleIds = Collections.unmodifiableList(ruleIds.toList()),
                matchedLength = range.length,
            )
        }
    }
}

/**
 * [TextRedactor.redact]의 결과입니다.
 *
 * [redactedText]는 입력과 같은 UTF-16 길이를 유지합니다. 이 결과는 발견하지 못한 민감정보가
 * 없음을 보장하지 않으며, 원문이나 매칭된 값을 metadata로 보관하지 않습니다.
 */
@ConsistentCopyVisibility
data class RedactionResult internal constructor(
    val redactedText: String,
    val spans: List<RedactionSpan>,
) : Serializable {

    /** merge된 span의 개수입니다. */
    val matchCount: Int
        get() = spans.size

    override fun toString(): String =
        "RedactionResult(length=${redactedText.length}, matchCount=$matchCount)"

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * keyword와 regex 규칙을 함께 적용하는 thread-safe redactor입니다.
 *
 * keyword 탐지는 기존 [AhoCorasickAutomaton]을 재사용하고 regex 탐지는 caller가 제공한
 * [Regex]를 사용합니다. 겹치는 span은 하나로 합치고 adjacent span은 분리하며, 합쳐진 span의
 * category는 priority가 가장 작은 규칙(동률이면 id, category 순서)을 따릅니다.
 */
class TextRedactor private constructor(
    private val policy: RedactionPolicy,
) {

    private val regexRules = policy.rules.filter { it.kind == RedactionRuleKind.REGEX }
    private val keywordRules = policy.rules.filter { it.kind == RedactionRuleKind.KEYWORD }

    private val keywordAutomaton: AhoCorasickAutomaton<RedactionRule>? =
        keywordRules.takeIf { it.isNotEmpty() }?.let { rules ->
            ahoCorasick<RedactionRule> {
                ignoreCase = policy.keywordIgnoreCase
                allowOverlaps = true
                normalization = policy.keywordNormalization
                rules.forEach { rule -> keyword(rule.keyword.orEmpty(), rule) }
            }
        }

    /** [text]를 redaction하고 원문 offset 기반 metadata를 반환합니다. */
    fun redact(text: CharSequence): RedactionResult {
        val source = text.toString()
        source.length.requireInRange(0, policy.maxTextLength, "text.length")
        if (source.isEmpty()) {
            return RedactionResult(source, emptyList())
        }

        val spans = mergeMatches(findMatches(source)).map { it.toSpan() }
        return RedactionResult(
            redactedText = maskText(source, spans),
            spans = Collections.unmodifiableList(spans),
        )
    }

    private fun findMatches(text: String): List<RedactionMatch> =
        regexRules.flatMap { rule -> rule.findRegexMatches(text) } +
            keywordAutomaton
                ?.parseText(text)
                .orEmpty()
                .map { match ->
                    RedactionMatch(
                        range = RedactionRange.of(match.start, match.end + 1),
                        rule = match.value,
                    )
                }

    private fun RedactionRule.findRegexMatches(text: String): List<RedactionMatch> =
        regex
            ?.findAll(text)
            ?.mapNotNull { match ->
                if (match.range.first >= match.range.last) {
                    null
                } else {
                    RedactionMatch(
                        range = RedactionRange.of(match.range.first, match.range.last + 1),
                        rule = this,
                    )
                }
            }
            ?.toList()
            .orEmpty()

    private fun mergeMatches(matches: List<RedactionMatch>): List<RedactionMergedMatch> {
        if (matches.isEmpty()) return emptyList()

        val sorted = matches.sortedWith(
            compareBy<RedactionMatch> { it.range.startInclusive }
                .thenByDescending { it.range.endExclusive }
                .thenBy { it.rule.priority }
                .thenBy { it.rule.id }
                .thenBy { it.rule.category },
        )
        val merged = mutableListOf<RedactionMergedMatch>()
        var current = RedactionMergedMatch.of(sorted.first())
        sorted.drop(1).forEach { next ->
            if (next.range.startInclusive < current.range.endExclusive) {
                current = current.merge(next)
            } else {
                merged += current
                current = RedactionMergedMatch.of(next)
            }
        }
        merged += current
        return merged
    }

    private fun RedactionMergedMatch.toSpan(): RedactionSpan =
        RedactionSpan.of(
            range = range,
            category = rules.sortedByPriority().first().category,
            ruleIds = rules.sortedByPriority().map { it.id }.distinct(),
        )

    private fun maskText(text: String, spans: List<RedactionSpan>): String {
        if (spans.isEmpty()) return text
        val chars = text.toCharArray()
        spans.forEach { span ->
            for (index in span.range.startInclusive until span.range.endExclusive) {
                chars[index] = policy.maskChar
            }
        }
        return chars.concatToString()
    }

    companion object {
        /** 기본 정책으로 redactor를 생성합니다. */
        @JvmStatic
        fun default(): TextRedactor = of(
            RedactionPolicy.of(
                listOf(
                    RedactionRule.regex(
                        id = "email",
                        category = "contact",
                        patternSource = """\b[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,253}\.[A-Za-z]{2,24}\b""",
                        priority = 10,
                    ),
                    RedactionRule.regex(
                        id = "phone",
                        category = "contact",
                        patternSource = """\b(?:\+?1[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}\b""",
                        priority = 20,
                    ),
                )
            )
        )

        /** caller 정책 snapshot으로 redactor를 생성합니다. */
        @JvmStatic
        fun of(policy: RedactionPolicy): TextRedactor = TextRedactor(policy)
    }
}

internal enum class RedactionRuleKind {
    KEYWORD,
    REGEX,
}

private data class RedactionMatch(
    val range: RedactionRange,
    val rule: RedactionRule,
)

private data class RedactionMergedMatch(
    val range: RedactionRange,
    val rules: List<RedactionRule>,
) {
    fun merge(next: RedactionMatch): RedactionMergedMatch =
        RedactionMergedMatch(
            range = RedactionRange.of(
                range.startInclusive.coerceAtMost(next.range.startInclusive),
                range.endExclusive.coerceAtLeast(next.range.endExclusive),
            ),
            rules = rules + next.rule,
        )

    companion object {
        fun of(match: RedactionMatch): RedactionMergedMatch =
            RedactionMergedMatch(match.range, listOf(match.rule))
    }
}

private fun List<RedactionRule>.sortedByPriority(): List<RedactionRule> =
    sortedWith(
        compareBy<RedactionRule> { it.priority }
            .thenBy { it.id }
            .thenBy { it.category },
    )

private val SAFE_METADATA_SLUG = Regex("^[a-z0-9._-]{1,64}$")
