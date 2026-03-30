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

import models.countries.{Country, CountryUk}
import models.requests.AddressDetails
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class IndWithoutIdAddressNonUkSpec extends AnyFreeSpec with Matchers with OptionValues {

  val indWithoutIdAddressNonUkFull = IndWithoutIdAddressNonUk(
    addressLine1 = "123 Main Street",
    addressLine2 = Some("Birmingham"),
    region = Some("Hamingburm"),
    townOrCity = "Townington",
    postcode = Some("123456"),
    country = Country(code = "FR", description = "France")
  )

  val indWithoutIdAddressNonUkEmpty = IndWithoutIdAddressNonUk(
    addressLine1 = "123 Main Street",
    addressLine2 = None,
    region = None,
    townOrCity = "Townington",
    postcode = None,
    country = Country(code = "FR", description = "France")
  )

  val indWithoutIdAddressNonUkEmptyNoAddressLine2 = IndWithoutIdAddressNonUk(
    addressLine1 = "123 Main Street",
    addressLine2 = None,
    region = Some("Sector Eight"),
    townOrCity = "Townington",
    postcode = Some("123456"),
    country = Country(code = "FR", description = "France")
  )

  "IndWithoutIdAddressNonUk" - {
    "toAddressDetailsNonUk" - {
      "should return address details when given a full address details" in {
        val result                 = indWithoutIdAddressNonUkFull.toAddressDetailsNonUk
        val expectedAddressDetails = AddressDetails(
          addressLine1 = indWithoutIdAddressNonUkFull.addressLine1,
          addressLine2 = indWithoutIdAddressNonUkFull.addressLine2,
          addressLine3 = indWithoutIdAddressNonUkFull.region,
          townOrCity = indWithoutIdAddressNonUkFull.townOrCity,
          postalCode = indWithoutIdAddressNonUkFull.postcode,
          countryCode = indWithoutIdAddressNonUkFull.country.code
        )
        result mustBe expectedAddressDetails
      }
      "should return address details when given an empty address details" in {
        val result                 = indWithoutIdAddressNonUkEmpty.toAddressDetailsNonUk
        val expectedAddressDetails = AddressDetails(
          addressLine1 = indWithoutIdAddressNonUkEmpty.addressLine1,
          addressLine2 = None,
          addressLine3 = None,
          townOrCity = indWithoutIdAddressNonUkEmpty.townOrCity,
          postalCode = None,
          countryCode = indWithoutIdAddressNonUkFull.country.code
        )
        result mustBe expectedAddressDetails
      }

      "should return address details with address line 3 shifted to address line 2 if address line 2 is None" in {
        val result                 = indWithoutIdAddressNonUkEmptyNoAddressLine2.toAddressDetailsNonUk
        val expectedAddressDetails = AddressDetails(
          addressLine1 = indWithoutIdAddressNonUkEmptyNoAddressLine2.addressLine1,
          addressLine2 = indWithoutIdAddressNonUkEmptyNoAddressLine2.region,
          addressLine3 = None,
          townOrCity = indWithoutIdAddressNonUkEmptyNoAddressLine2.townOrCity,
          postalCode = indWithoutIdAddressNonUkEmptyNoAddressLine2.postcode,
          countryCode = indWithoutIdAddressNonUkEmptyNoAddressLine2.country.code
        )
        result mustBe expectedAddressDetails
      }
    }
  }
}
