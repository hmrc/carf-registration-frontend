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

import models.countries.CountryUk
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class AddressUkSpec extends AnyFreeSpec with Matchers with OptionValues {

  val ukAddressFull = AddressUk(
    addressLine1 = "123 Main Street",
    addressLine2 = Some("Birmingham"),
    addressLine3 = Some("Hamingburm"),
    townOrCity = "Townington",
    postCode = "B23 2AZ",
    countryUk = CountryUk(code = "GB", name = "United Kingdom")
  )

  val ukAddressMinimal = AddressUk(
    addressLine1 = "123 Main Street",
    addressLine2 = None,
    addressLine3 = None,
    townOrCity = "Townington",
    postCode = "B23 2AZ",
    countryUk = CountryUk(code = "GB", name = "United Kingdom")
  )

  val ukAddressEmptyStrings = AddressUk(
    addressLine1 = "123 Main Street",
    addressLine2 = Some(""),
    addressLine3 = Some(""),
    townOrCity = "Townington",
    postCode = "B23 2AZ",
    countryUk = CountryUk(code = "GB", name = "United Kingdom")
  )

  "AddressUk" - {
    "renderHTML" - {
      "must render UK address correctly" in {
        val result = ukAddressFull.renderHTML

        result must include("123 Main Street")
        result must include("Birmingham")
        result must include("Hamingburm")
        result must include("Townington")
        result must include("B23 2AZ")
        result must include("United Kingdom")
        result must include("<br>")
        result must include("""<span class="govuk-!-margin-bottom-0">""")
      }

      "must handle minimal address with only required fields" in {
        val result = ukAddressMinimal.renderHTML

        result mustEqual """<span class="govuk-!-margin-bottom-0">123 Main Street</span><br><span class="govuk-!-margin-bottom-0">Townington</span><br><span class="govuk-!-margin-bottom-0">B23 2AZ</span><br>United Kingdom"""
      }

      "must filter out empty optional fields" in {
        val result = ukAddressEmptyStrings.renderHTML

        result must include("123 Main Street")
        result must not include """<span class="govuk-!-margin-bottom-0"></span>"""
      }
    }
  }
}
