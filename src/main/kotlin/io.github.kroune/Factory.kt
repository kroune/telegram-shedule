package io.github.kroune

import io.github.kroune.alerts.AlertsRepositoryI
import io.github.kroune.alerts.TgAlertsRepositoryImpl
import io.github.kroune.configuration.ConfigurationRepositoryI
import io.github.kroune.configuration.ConfigurationRepositoryImpl
import io.github.kroune.googleSheetsService.GoogleSheetsRepositoryServiceI
import io.github.kroune.googleSheetsService.GoogleSheetsServiceRepositoryImpl
import io.github.kroune.translation.RussianTranslationRepositoryImpl
import io.github.kroune.translation.TranslationRepositoryI
import io.github.kroune.unparsedSchedule.UnparsedScheduleRepositoryI
import io.github.kroune.unparsedSchedule.UnparsedScheduleRepositoryImpl
import io.github.kroune.unparsedScheduleParser.ParserRepositoryI
import io.github.kroune.unparsedScheduleParser.ParserRepositoryImpl


/**
 * WARNING: THIS SHIT SHOULD BE BEFORE ANY OTHER REPOSITORIES, OR YOU WILL GET FUCKED (NPE)
 * IF REPOSITORIES ABOVE THIS ONE GET SOME ERROR (AND SO WILL TRY TO NOTIFY ABOUT IT) [alertsRepository] WILL BE NULL AT
 * THIS MOMENT
 *
 * provides repository for sending Telegram alerts (if something goes wrong with bot)
 */
val alertsRepository: AlertsRepositoryI = TgAlertsRepositoryImpl()

/**
 * provides repository for getting unparsed schedule
 */
val unparsedScheduleRepository: UnparsedScheduleRepositoryI = UnparsedScheduleRepositoryImpl()

/**
 * provides repository for parsing unparsed schedule
 */
val parserRepository: ParserRepositoryI = ParserRepositoryImpl()

/**
 * provides repository for Google Sheets service
 */
val googleSheetsServiceRepository: GoogleSheetsRepositoryServiceI = GoogleSheetsServiceRepositoryImpl()

/**
 * provides repository for storing user configuration
 */
val configurationRepository: ConfigurationRepositoryI = ConfigurationRepositoryImpl()

/**
 * provides repository for message translations
 */
val translationRepository: TranslationRepositoryI = RussianTranslationRepositoryImpl()
