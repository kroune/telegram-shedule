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
