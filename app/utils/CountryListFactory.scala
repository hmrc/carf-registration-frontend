/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import config.FrontendAppConfig
import models.countries.*
import play.api.Environment
import play.api.libs.json.Json
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem

import javax.inject.{Inject, Singleton}

@Singleton
class CountryListFactory @Inject() (environment: Environment, appConfig: FrontendAppConfig) {

  final def ukCountries: Seq[Country] = countryCodesForUkCountries.collect {
    case x if x.code == "UK" => GB
    case x                   => x
  }.toSeq

  def countryCodesForUkCountries: Set[Country]              = Set(GB, UK, GG, JE, IM)
  final lazy val countryCodesForUkCountryCodes: Set[String] = countryCodesForUkCountries.map(_.code)

  def countryList: Option[Seq[Country]] = getCountryList

  private def getCountryList: Option[Seq[Country]] =
    environment.resourceAsStream(appConfig.countryCodeJson) map Json.parse map {
      _.as[Seq[Country]]
        .map(country =>
          if (country.alternativeName.isEmpty) {
            val updatedCountry = Country(country.code, country.description, Option(country.description))
            updatedCountry
          } else {
            country
          }
        )
        .sortWith((country, country2) => country.description.toLowerCase < country2.description.toLowerCase)
    }

  def getDescriptionFromCode(code: String): Option[String] = countryList flatMap {
    _.find((p: Country) => p.code == code).map(_.description)
  }

  // TODO redo: methods should not be used in main code and for testing purposes makes it redundant

  lazy val countryListWithoutGB: Option[Seq[Country]] = countryList.map {
    _.filter(_.code != "GB")
  }

  lazy val countryListWithoutUKCountries: Option[Seq[Country]] = countryList.map { countries =>
    countries.filter(country => !countryCodesForUkCountryCodes.contains(country.code))
  }

  lazy val countryListWithUKCountries: Option[Seq[Country]] = countryList.map { countries =>
    countries.filter(country => countryCodesForUkCountryCodes.contains(country.code))
  }

  def countrySelectList(value: Map[String, String], countries: Seq[Country]): Seq[SelectItem] = {
    val countryJsonList = countries
      .groupBy(_.code)
      .map { case (_, countries) =>
        val country    = countries.head
        val names      = countries.flatMap(c => List(Some(c.description), c.alternativeName)).flatten.distinct
        val isSelected = value.get("country").contains(country.code)
        SelectItem(
          Some(country.code),
          country.description,
          isSelected,
          attributes = Map("data-text" -> (if (isSelected) country.description else names.mkString(":")))
        )
      }
      .toSeq
      .sortBy(_.text)
    SelectItem(Some(""), "Select a country", selected = false) +: countryJsonList
  }

}
