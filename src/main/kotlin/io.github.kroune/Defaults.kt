package io.github.kroune

import io.github.kroune.updater.EditPreviousOnChange
import io.github.kroune.updater.UpdateI

/**
 * is user didn't specify output mode, this mode will be used
 */
val defaultOutputMode: UpdateI = EditPreviousOnChange()
