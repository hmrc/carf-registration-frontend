/*
 * Copyright 2025 HM Revenue & Customs
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

package models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class IndividualDetailsSpec extends AnyFreeSpec with Matchers {

  val ukAddress = Address(
    addressLine1 = "123 Street",
    addressLine2 = Some("London"),
    addressLine3 = None,
    addressLine4 = None,
    postalCode = Some("SKKA 1BB"),
    countryCode = "GB"
  )

  val nonUkAddress = Address(
    addressLine1 = "321 Lane",
    addressLine2 = Some("New York"),
    addressLine3 = None,
    addressLine4 = None,
    postalCode = Some("18801"),
    countryCode = "US"
  )

  val testIndividualDetails: IndividualDetails = IndividualDetails(
    safeId = "testSafeId",
    firstName = "Tim",
    lastName = "Jones",
    middleName = Some("Jim"),
    address = ukAddress
  )

  "IndividualDetails" - {

    "isUkBased" - {
      "must return true for GB country code" in {
        val testDetails = testIndividualDetails.copy(address = ukAddress)

        testDetails.isUkBased mustEqual true
      }

      "must return false for non-GB country codes" in {
        val testDetails = testIndividualDetails.copy(address = nonUkAddress)

        testDetails.isUkBased mustEqual false
      }

      "must return false for empty country code" in {
        val emptyCountryAddress = ukAddress.copy(countryCode = "")
        val testDetails = testIndividualDetails.copy(address = emptyCountryAddress)

        testDetails.isUkBased mustEqual false
      }

      "must be case sensitive" in {
        val emptyCountryAddress = ukAddress.copy(countryCode = "gb")
        val testDetails = testIndividualDetails.copy(address = emptyCountryAddress)

        testDetails.isUkBased mustEqual false
      }
    }
    "fullName" - {
      "should display the whole name when middle name is populated" in {
        val testWholeName: IndividualDetails = IndividualDetails(
          safeId = "testSafeId",
          firstName = "Tim",
          lastName = "Jones",
          middleName = Some("Jim"),
          address = ukAddress
        )

        val result = testWholeName.fullName

        result mustEqual "Tim Jim Jones"
      }
      "should display just the first and last name when there is no middle name" in {
        val testEmptyName: IndividualDetails = IndividualDetails(
          safeId = "testSafeId",
          firstName = "Robert",
          lastName = "Kraut",
          middleName = None,
          address = ukAddress
        )

        val result = testEmptyName.fullName

        result mustEqual "Robert Kraut"
      }
    }
  }
}
