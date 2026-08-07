# Input safety

Validate untrusted text before tokenizer, dictionary, or model work. The 0.3.0 request models cap tokenizer and blockword text at `100_000` characters and reject blank input.

![Request safety boundary](../../assets/operations/request-safety-boundary.png)

## Map boundary failures explicitly

```kotlin
import io.bluetape4k.tokenizer.model.MAX_TOKENIZE_TEXT_LENGTH
import io.bluetape4k.tokenizer.model.tokenizeRequestOf

fun tokenizeStatus(text: String): Int =
    when {
        text.isBlank() -> 400
        text.length > MAX_TOKENIZE_TEXT_LENGTH -> 413
        else -> {
            tokenizeRequestOf(text)
            200
        }
    }
```

The adapter checks the same public constant as the model. Model validation remains the final guard if a caller bypasses the adapter.

## Do not echo submitted text

An invalid input may contain credentials, messages, or regulated data. Error bodies and logs should contain only:

- failure category;
- actual character length;
- configured maximum;
- a request or trace identifier governed by application policy.

The repository safety example deliberately reports metadata rather than raw text.

## Separate invalid input from processor failure

| Case | Suggested HTTP mapping | Safe message |
|---|---:|---|
| blank text | 400 | `text is blank` |
| over maximum length | 413 | `text too long: <length> chars (max <limit>)` |
| unexpected processor exception | 500 | `processor error` |

Do not collapse these into one retryable error. A caller can correct 400/413 input, while a processor failure needs service diagnosis.

## Coroutines and cancellation

If the application calls processing inside a suspend boundary, rethrow `CancellationException` before broad exception handling. The runnable 0.3.0 safety example is synchronous around processors and uses `runCatching`; coroutine services should not copy that pattern around suspend calls because cancellation must propagate.

## Add application limits when needed

The library maximum is a safety ceiling, not a performance target. An endpoint may choose a smaller limit based on latency, memory, or product requirements. Apply byte limits during transport parsing as well as character limits after decoding.

## Verify the boundary

Test blank, whitespace-only, exactly-at-limit, one-over-limit, processor exception, and sanitized output. The [tokenizer safety example](../examples/tokenizer-safety-examples.md) supplies a runnable starting point.

## Source evidence

- [TokenizeRequest limits](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-core/src/main/kotlin/io/bluetape4k/tokenizer/model/TokenizeRequest.kt)
- [BlockwordRequest limits](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/tokenizer-core/src/main/kotlin/io/bluetape4k/tokenizer/model/BlockwordRequest.kt)
- [Safety example](https://github.com/bluetape4k/bluetape4k-text/blob/0.3.0/examples/tokenizer-safety-examples/src/main/kotlin/io/bluetape4k/text/examples/tokenizer/TokenizerSafetyExamples.kt)
