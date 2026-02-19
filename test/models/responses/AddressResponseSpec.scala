/*
 * Copyright 2026 HM Revenue & Customs
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

package models.responses

import base.SpecBase
import models.AddressUk
import models.countries.CountryUk
import models.error.ConversionError

class AddressResponseSpec extends SpecBase {

  def createAddressResponse(lines: List[String]) =
    AddressResponse(
      id = "test id",
      address = AddressRecord(
        lines = lines,
        town = "Testington",
        postcode = testPostcode,
        country = CountryRecord(code = "GB", name = "United Kingdom")
      )
    )

  "AddressResponse" - {
    "toDomainAddressUk method" - {
      "should successfully convert the address api response to a domain model" - {
        "when lines has one value" in {
          val testAddressResponse = createAddressResponse(lines = List("Line 1"))

          val result = testAddressResponse.toDomainAddressUk

          result mustBe Right(
            AddressUk(
              addressLine1 = "Line 1",
              addressLine2 = None,
              addressLine3 = None,
              townOrCity = "Testington",
              postCode = testPostcode,
              countryUk = CountryUk(code = "GB", name = "United Kingdom")
            )
          )
        }
        "when lines has two values" in {
          val testAddressResponse = createAddressResponse(lines = List("Line 1", "Line 2"))

          val result = testAddressResponse.toDomainAddressUk

          result mustBe Right(
            AddressUk(
              addressLine1 = "Line 1",
              addressLine2 = Some("Line 2"),
              addressLine3 = None,
              townOrCity = "Testington",
              postCode = testPostcode,
              countryUk = CountryUk(code = "GB", name = "United Kingdom")
            )
          )
        }
        "when lines has three values" in {
          val testAddressResponse = createAddressResponse(lines = List("Line 1", "Line 2", "Line 3"))

          val result = testAddressResponse.toDomainAddressUk

          result mustBe Right(
            AddressUk(
              addressLine1 = "Line 1",
              addressLine2 = Some("Line 2"),
              addressLine3 = Some("Line 3"),
              townOrCity = "Testington",
              postCode = testPostcode,
              countryUk = CountryUk(code = "GB", name = "United Kingdom")
            )
          )
        }
        "when lines has four values, only populate the first three because lines has a max size of three in the API spec" in {
          val testAddressResponse = createAddressResponse(lines = List("Line 1", "Line 2", "Line 3", "Line 4"))

          val result = testAddressResponse.toDomainAddressUk

          result mustBe Right(
            AddressUk(
              addressLine1 = "Line 1",
              addressLine2 = Some("Line 2"),
              addressLine3 = Some("Line 3"),
              townOrCity = "Testington",
              postCode = testPostcode,
              countryUk = CountryUk(code = "GB", name = "United Kingdom")
            )
          )
        }
      }
      "should return a ConversionError when lines is empty" in {
        val testAddressResponse = createAddressResponse(lines = List.empty)

        val result = testAddressResponse.toDomainAddressUk

        result mustBe Left(ConversionError)
      }
    }
  }
}
