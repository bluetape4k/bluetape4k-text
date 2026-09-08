package io.bluetape4k.text.search.benchmark

import com.sun.management.ThreadMXBean
import java.lang.management.ManagementFactory

/** 같은 스레드의 반복 공개 조회에서 할당한 바이트를 측정합니다. 초기 사전 로딩은 제외합니다. */
object DictionaryAccessorAllocationDiagnostic {

    @Volatile
    private var sink: Any? = null

    @JvmStatic
    fun main(args: Array<String>) {
        val bean = ManagementFactory.getThreadMXBean() as ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported)
        bean.isThreadAllocatedMemoryEnabled = true
        val benchmarks = DictionaryAccessorBenchmark()
        benchmarks.setup()
        val accessors = linkedMapOf<String, () -> Any>(
            "koreanDictionary" to benchmarks::koreanDictionary,
            "koreanBlockwords" to benchmarks::koreanBlockwords,
            "koreanProperNouns" to benchmarks::koreanProperNouns,
            "japaneseBlockwords" to benchmarks::japaneseBlockwords,
            "koreanSnapshot" to benchmarks::koreanSnapshot,
            "japaneseSnapshot" to benchmarks::japaneseSnapshot,
        )
        accessors.forEach { (name, accessor) ->
            repeat(10) { sink = accessor() }
            val before = bean.getCurrentThreadAllocatedBytes()
            repeat(30) { sink = accessor() }
            val bytesPerOperation = (bean.getCurrentThreadAllocatedBytes() - before) / 30.0
            println("ISSUE334_ALLOCATION accessor=$name bytesPerOperation=$bytesPerOperation")
        }
    }
}
