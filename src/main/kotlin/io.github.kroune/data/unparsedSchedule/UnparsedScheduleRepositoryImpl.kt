package data.unparsedSchedule

import com.google.api.services.sheets.v4.model.ValueRange
import data.googleSheetsServiceRepository

/**
 * Gets schedule from Google sheets API
 */
class UnparsedScheduleRepositoryImpl: UnparsedScheduleRepositoryI {

    override suspend fun getSchedule(): List<List<String>> {
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
        return values.map { it.map { it.toString() } }
    }
}
