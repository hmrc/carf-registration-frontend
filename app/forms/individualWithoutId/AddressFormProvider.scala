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

package forms.individualWithoutId

import config.Constants.{addressRegex, postCodeAllowedChars, regexPostcode}
import forms.mappings.Mappings
import models.AddressUK
import play.api.data.Form
import play.api.data.Forms.*

import javax.inject.Inject

class AddressFormProvider @Inject() extends Mappings {

  inline val maxLength = 35

  def apply(): Form[AddressUK] = Form(
    mapping(
      "addressLine1" -> text("address.addressLine1.error.required").verifying(
        maxLength(35, "address.addressLine1.error.length"),
        regexp(addressRegex, "address.addressLine1.error.invalid")
      ),
      "addressLine2" -> optional(
        text("address.addressLine2.error.required")
          .verifying(
            maxLength(35, "address.addressLine2.error.length"),
            regexp(addressRegex, "address.addressLine2.error.invalid")
          )
      ),
      "townOrCity"   -> text("address.townOrCity.error.required").verifying(
        maxLength(35, "address.townOrCity.error.length"),
        regexp(addressRegex, "address.townOrCity.error.invalid")
      ),
      "county"       -> optional(
        text("address.county.error.required")
          .verifying(
            maxLength(35, "address.county.error.length"),
            regexp(addressRegex, "address.county.error.invalid")
          )
      ),
      "postcode"     -> mandatoryPostcode(
        "address.postcode.error.required",
        "address.postcode.error.length",
        "address.postcode.error.invalidFormat",
        regexPostcode,
        "address.postcode.error.invalid",
        postCodeAllowedChars,
        Some("address.postcode.error.notReal")
      ),
      "country"      -> text("address.country.error.required")
    )(AddressUK.apply)(x => Some(x.addressLine1, x.addressLine2, x.townOrCity, x.county, x.postCode, x.countryCode))
  )
}
