package io.github.kroune.unparsedSchedule

import com.google.api.services.sheets.v4.model.ValueRange
import io.github.kroune.googleSheetsServiceRepository
import io.github.kroune.retryableExitedOnFatal
import kotlin.time.Duration.Companion.seconds

/**
 * Gets schedule from Google sheets API
 */
class UnparsedScheduleRepositoryImpl : UnparsedScheduleRepositoryI {

    override suspend fun getSchedule(): List<List<String>> {
        return {
            // Build a new authorized API client service.
            val spreadsheetId = "1L9UjNOZx4p4VER11SCyU97M07QnfWsZWwldAAOR0gtM"
            val spreadsheet = googleSheetsServiceRepository.service.spreadsheets().get(spreadsheetId).execute()
            val allSheets = spreadsheet.sheets
            val firstSheetName = allSheets.first().properties.title
            val range = "$firstSheetName"
            val data: ValueRange = googleSheetsServiceRepository.service.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute()
            val values: List<List<Any>>? = data.getValues()
            require(values != null)
            require(values.isNotEmpty())
            values.map { it.map { it.toString() } }
        }.retryableExitedOnFatal(retries = 30, delay = 20.seconds)
        // we can use such delay, since schedule updates can be delayed without much of a damage
    }
}
