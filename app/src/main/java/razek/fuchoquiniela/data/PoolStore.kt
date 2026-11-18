package razek.fuchoquiniela.data

/** The pools from the last list response, so pool screens can be opened by uuid. */
object PoolStore {
    private val pools = mutableMapOf<String, Pool>()

    fun put(list: List<Pool>) {
        list.forEach { pools[it.uuid] = it }
    }

    fun put(pool: Pool) {
        pools[pool.uuid] = pool
    }

    operator fun get(uuid: String): Pool? = pools[uuid]

    fun clear() = pools.clear()
}
