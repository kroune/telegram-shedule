package io.github.kroune

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * filters elements and casts it to provided type, compiler doesn't smart cast filters results
 *
 * for example, you filter null values, and you don't want to add !!
 */
inline fun <T, C : MutableCollection<K>, K> Iterable<T>.unsafeFilterTo(destination: C, predicate: (T) -> Boolean): C {
    @Suppress("UNCHECKED_CAST")
    for (element in this) if (predicate(element)) destination.add(element as K)
    return destination
}

/**
 * drops elements until predicate returns true, drops 1 more element and then returns
 */
inline fun <T> Iterable<T>.dropWhileInclusive(predicate: (T) -> Boolean): List<T> {
    var yielding = false
    val list = ArrayList<T>()
    for (item in this)
        if (yielding)
            list.add(item)
        else if (!predicate(item)) {
            yielding = true
        }
    return list
}

/**
 * maps elements as a pair of index and element
 */
fun <T> Iterable<T>.mapWithIndexes(): List<Pair<Int, T>> {
    return this.mapIndexed { index, element ->
        Pair(index, element)
    }
}

/**
 * adds [element] to the collection [count] times
 */
fun <T> MutableCollection<T>.repeatedAdd(element: T, count: Int) {
    repeat(count) {
        this.add(element)
    }
}

/**
 * maps elements as a pair of index and element and performs operation on element
 */
inline fun <T, R> Iterable<T>.mapWithIndexes(transform: (T) -> R): List<Pair<Int, R>> {
    return this.mapIndexed { index, element ->
        Pair(index, transform(element))
    }
}


/**
 * exits if exceeded the maximum number of retries
 * @param F any suspend function
 * @param T result of the function
 * @param retries maximum number of retries
 * @param delay delay between retries
 */
suspend fun <F : suspend () -> T, T> F.retryableExitedOnFatal(
    retries: Int = 3,
    delay: Duration = 850.milliseconds,
): T {
    return suspendRetryable(retries, delay, true, true).getOrThrow()
}

/**
 * exits if exceeded the maximum number of retries
 * @param F any function
 * @param T result of the function
 * @param retries maximum number of retries
 * @param delay delay between retries
 */
fun <F : () -> T, T> F.retryableExitedOnFatal(
    retries: Int = 3,
    delay: Duration = 850.milliseconds,
): T {
    return retryable(retries, delay, true, true).getOrThrow()
}

/**
 * @param F any function
 * @param T result of the function
 * @param retries maximum number of retries
 * @param delay delay between retries
 * @param notifyAboutFailure if it should notify me about failures
 * @param exitOnFatal if it should exit the program if we exceed the maximum number of retries
 */
fun <F : () -> T, T> F.retryable(
    retries: Int = 3,
    delay: Duration = 850.milliseconds,
    notifyAboutFailure: Boolean = true,
    exitOnFatal: Boolean
): Result<T> {
    return runBlocking {
        suspend { this@retryable() }.suspendRetryable(retries, delay, notifyAboutFailure, exitOnFatal)
    }
}

/**
 * retries the function until it succeeds or the maximum number of retries is reached
 * @param F any suspend function
 * @param T result of the function
 * @param retries maximum number of retries
 * @param delay delay between retries
 * @param notifyAboutFailure if it should notify me about failures
 * @param exitOnFatal if it should exit the program if we exceed the maximum number of retries
 */
suspend fun <F : suspend () -> T, T> F.suspendRetryable(
    retries: Int = 3,
    delay: Duration = 850.milliseconds,
    notifyAboutFailure: Boolean = true,
    exitOnFatal: Boolean
): Result<T> {
    require(retries >= 0) { "Retries must be greater or equal to 0" }
    var lastException: Throwable? = null
    repeat(1 + retries) {
        val result = runCatching {
            this@suspendRetryable()
        }
        if (result.isSuccess)
            return result

        lastException = result.exceptionOrNull()!!
        runBlocking {
            if (notifyAboutFailure) {
                alert("Error while executing function: ${this@suspendRetryable.javaClass.name}").join()
                alert(result.exceptionOrNull()!!.stackTraceToString()).join()
            }
            delay(delay)
        }
    }
    if (exitOnFatal) {
        runBlocking {
            alert("Error executing function: ${this@suspendRetryable.javaClass.name}, retried $retries times").join()
        }
        exitProcess(-1)
    } else {
        return Result.failure(lastException!!)
    }
}

/**
 * Alerts me using [alertsRepository]
 */
fun alert(message: String): Job {
    return alertsRepository.alert(message)
}


/**
 * Alerts me using [alertsRepository]
 */
fun alert(message: () -> String): Job {
    return alert(message())
}
