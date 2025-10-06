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
import play.api.libs.json.Json
import org.scalatest.OptionValues

class AddressSpec extends AnyFreeSpec with Matchers with OptionValues {

  "Address" - {

    val ukAddress = Address(
      addressLine1 = "123 Main Street",
      addressLine2 = Some("Birmingham"),
      addressLine3 = None,
      addressLine4 = None,
      postalCode = Some("B23 2AZ"),
      countryCode = "GB"
    )

    val nonUkAddress = Address(
      addressLine1 = "321 Pear",
      addressLine2 = Some("New York"),
      addressLine3 = Some("NY"),
      addressLine4 = None,
      postalCode = Some("10771"),
      countryCode = "US"
    )

    "renderHTML" - {

      "must render UK address correctly without country code" in {
        val result = ukAddress.renderHTML(isUkBased = true)

        result must include("123 Main Street")
        result must include("Birmingham")
        result must include("B23 2AZ")
        result must include("<br>")
        result must include("""<span class="govuk-!-margin-bottom-0">""")
        result must not include "GB"
      }

      "must render non-UK address correctly with country code" in {
        val result = nonUkAddress.renderHTML(isUkBased = false)

        result must include("321 Pear")
        result must include("New York")
        result must include("NY")
        result must include("10771")
        result must include("US")
        result must include("<br>")
        result must include("""<span class="govuk-!-margin-bottom-0">""")
      }

      "must handle minimal address with only required fields" in {
        val minimalAddress = Address(
          addressLine1 = "1 Apple Street",
          addressLine2 = None,
          addressLine3 = None,
          addressLine4 = None,
          postalCode = None,
          countryCode = "FR"
        )

        val result = minimalAddress.renderHTML(isUkBased = false)

        result mustEqual """<span class="govuk-!-margin-bottom-0">1 Apple Street</span><br>FR"""
      }

      "must filter out empty optional fields" in {
        val addressWithEmpties = Address(
          addressLine1 = "123 Orange Street",
          addressLine2 = Some(""),
          addressLine3 = Some("Valid Line"),
          addressLine4 = Some(""),
          postalCode = Some("12345"),
          countryCode = "DE"
        )

        val result = addressWithEmpties.renderHTML(isUkBased = false)

        result must include("123 Orange Street")
        result must include("Valid Line")
        result must include("12345")
        result must include("DE")
        result must not include """<span class="govuk-!-margin-bottom-0"></span>"""
      }
    }

    "JSON serialization" - {

      "must serialize to JSON correctly" in {
        val json = Json.toJson(ukAddress)

        (json \ "addressLine1").as[String] mustEqual "123 Main Street"
        (json \ "addressLine2").as[String] mustEqual "Birmingham"
        (json \ "postalCode").as[String]   mustEqual "B23 2AZ"
        (json \ "countryCode").as[String]  mustEqual "GB"
      }

      "must deserialize from JSON correctly" in {
        val json = Json.obj(
          "addressLine1" -> "778 Apple Road",
          "addressLine2" -> "Apple City",
          "addressLine3" -> null,
          "addressLine4" -> null,
          "postalCode"   -> "T44T 1SS",
          "countryCode"  -> "GB"
        )

        val address = json.as[Address]

        address.addressLine1 mustEqual "778 Apple Road"
        address.addressLine2 mustEqual Some("Apple City")
        address.addressLine3 mustEqual None
        address.addressLine4 mustEqual None
        address.postalCode   mustEqual Some("T44T 1SS")
        address.countryCode  mustEqual "GB"
      }
    }
  }
}
