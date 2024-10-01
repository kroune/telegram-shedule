package data.googleSheetsService

import com.google.api.services.sheets.v4.Sheets

/**
 * Repository for accessing Google Sheets API service
 */
sealed interface GoogleSheetsRepositoryServiceI {
    /**
     * Google Sheets API service
     */
    val service: Sheets
}
