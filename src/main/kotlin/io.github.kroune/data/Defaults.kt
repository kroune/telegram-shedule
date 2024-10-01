package data

import data.updater.EditPreviousOnChange
import data.updater.UpdateI

/**
 * is user didn't specify output mode, this mode will be used
 */
val defaultOutputMode: UpdateI = EditPreviousOnChange()
