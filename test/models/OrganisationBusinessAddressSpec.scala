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

class OrganisationBusinessAddressSpec extends AnyFreeSpec with Matchers with OptionValues {

  val organisationBusinessAddressFull = OrganisationBusinessAddress(
    addressLine1 = "123 Main Street",
    addressLine2 = Some("Birmingham"),
    region = Some("Hamingburm"),
    townOrCity = "Townington",
    postcode = Some("123456"),
    country = Country(code = "FR", description = "France")
  )

  val organisationBusinessAddressEmpty = OrganisationBusinessAddress(
    addressLine1 = "123 Main Street",
    addressLine2 = None,
    region = None,
    townOrCity = "Townington",
    postcode = None,
    country = Country(code = "FR", description = "France")
  )

  val organisationBusinessAddressNoAddressLine2 = OrganisationBusinessAddress(
    addressLine1 = "123 Main Street",
    addressLine2 = None,
    region = Some("Sector Eight"),
    townOrCity = "Townington",
    postcode = Some("123456"),
    country = Country(code = "FR", description = "France")
  )

  "OrganisationBusinessAddress" - {
    "toAddressDetailsOrg" - {
      "should return address details when given a full address details" in {
        val result                 = organisationBusinessAddressFull.toAddressDetailsOrg
        val expectedAddressDetails = AddressDetails(
          addressLine1 = organisationBusinessAddressFull.addressLine1,
          addressLine2 = organisationBusinessAddressFull.addressLine2,
          addressLine3 = organisationBusinessAddressFull.region,
          townOrCity = organisationBusinessAddressFull.townOrCity,
          postalCode = organisationBusinessAddressFull.postcode,
          countryCode = organisationBusinessAddressFull.country.code
        )
        result mustBe expectedAddressDetails
      }
      "should return address details when given an empty address details" in {
        val result                 = organisationBusinessAddressEmpty.toAddressDetailsOrg
        val expectedAddressDetails = AddressDetails(
          addressLine1 = organisationBusinessAddressEmpty.addressLine1,
          addressLine2 = None,
          addressLine3 = None,
          townOrCity = organisationBusinessAddressEmpty.townOrCity,
          postalCode = None,
          countryCode = organisationBusinessAddressEmpty.country.code
        )
        result mustBe expectedAddressDetails
      }

      "should return address details with address line 3 shifted to address line 2 if address line 2 is None" in {
        val result                 = organisationBusinessAddressNoAddressLine2.toAddressDetailsOrg
        val expectedAddressDetails = AddressDetails(
          addressLine1 = organisationBusinessAddressNoAddressLine2.addressLine1,
          addressLine2 = organisationBusinessAddressNoAddressLine2.region,
          addressLine3 = None,
          townOrCity = organisationBusinessAddressNoAddressLine2.townOrCity,
          postalCode = organisationBusinessAddressNoAddressLine2.postcode,
          countryCode = organisationBusinessAddressNoAddressLine2.country.code
        )
        result mustBe expectedAddressDetails
      }
    }
  }
}
