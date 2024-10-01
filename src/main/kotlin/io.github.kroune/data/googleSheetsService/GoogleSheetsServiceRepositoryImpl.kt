package data.googleSheetsService

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.Sheets.Builder
import com.google.api.services.sheets.v4.SheetsScopes
import java.io.File

/**
 * Provides Google Sheets service, using credentials.json file in resources folder
 */
class GoogleSheetsServiceRepositoryImpl: GoogleSheetsRepositoryServiceI {
    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val applicationName = "Google Sheets API for schedule extractor"
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val tokensStoreDirectoryPath = "tokens"

    private val accessScopes = mutableListOf<String?>(SheetsScopes.SPREADSHEETS_READONLY)
    private val credentialsFilePath = System.getenv("credentialsFilePath")

    private fun getCredentials(httpTransport: NetHttpTransport?): Credential {
        val credentials = File(credentialsFilePath).reader()
        val clientSecrets = GoogleClientSecrets.load(jsonFactory, credentials)

        val flow = GoogleAuthorizationCodeFlow.Builder(
            httpTransport, jsonFactory, clientSecrets, accessScopes
        )
            .setDataStoreFactory(FileDataStoreFactory(File(tokensStoreDirectoryPath)))
            .setAccessType("offline")
            .build()
        val receiver = LocalServerReceiver.Builder().setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }

    override val service: Sheets =
        Builder(httpTransport, jsonFactory, getCredentials(httpTransport))
            .setApplicationName(applicationName)
            .build()
}
