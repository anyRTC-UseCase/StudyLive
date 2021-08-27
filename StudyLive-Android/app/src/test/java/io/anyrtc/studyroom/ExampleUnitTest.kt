package io.anyrtc.studyroom

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val list = mutableListOf(0, 1, 2, 3, 4, 5)
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next > 1)
                iterator.remove()
        }

        println(list)
    }

    private class AnotherListener {

        fun invokeIt(itInterface: ExamInterface): AnotherListener {
            itInterface.invokeIt()
            return this
        }
    }

    fun interface ExamInterface {
        fun invokeIt()
    }
}