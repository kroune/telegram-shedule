package data

import data.configuration.ConfigurationRepositoryI
import data.configuration.ConfigurationRepositoryImpl
import data.googleSheetsService.GoogleSheetsRepositoryServiceI
import data.googleSheetsService.GoogleSheetsServiceRepositoryImpl
import data.translation.RussianTranslationRepositoryImpl
import data.translation.TranslationRepositoryI
import data.unparsedSchedule.UnparsedScheduleRepositoryI
import data.unparsedSchedule.UnparsedScheduleRepositoryImpl
import data.unparsedScheduleParser.ParserRepositoryI
import data.unparsedScheduleParser.ParserRepositoryImpl

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
