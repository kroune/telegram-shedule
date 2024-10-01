package io.github.kroune.alerts

import kotlinx.coroutines.Job

/**
 * Interface for handling alerts
 */
sealed interface AlertsRepositoryI {
    /**
     * alerts me if something goes wrong
     */
    fun alert(message: String): Job
}
