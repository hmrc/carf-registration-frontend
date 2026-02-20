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
import models.AddressUk
import models.countries.{Country, CountryUk}
import play.api.data.Form
import play.api.data.Forms.*

import javax.inject.Inject

class IndWithoutIdAddressFormProvider @Inject() extends Mappings {

  inline val maxLength = 35

  def apply(countryList: Seq[Country]): Form[AddressUk] = Form(
    mapping(
      "addressLine1" -> text("address.addressLine1.error.required").verifying(
        firstError(
          maxLength(maxLength, "address.addressLine1.error.length"),
          regexp(addressRegex, "address.addressLine1.error.invalid")
        )
      ),
      "addressLine2" -> optional(
        text("address.addressLine2.error.required")
          .verifying(
            firstError(
              maxLength(maxLength, "address.addressLine2.error.length"),
              regexp(addressRegex, "address.addressLine2.error.invalid")
            )
          )
      ),
      "addressLine3" -> optional(
        text("address.addressLine3.error.required")
          .verifying(
            firstError(
              maxLength(maxLength, "address.addressLine3.error.length"),
              regexp(addressRegex, "address.addressLine3.error.invalid")
            )
          )
      ),
      "townOrCity"   -> text("address.townOrCity.error.required").verifying(
        firstError(
          maxLength(maxLength, "address.townOrCity.error.length"),
          regexp(addressRegex, "address.townOrCity.error.invalid")
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
      "country"      -> countryUkMapping(countryList)
    )(AddressUk.apply)(x => Some(x.addressLine1, x.addressLine2, x.addressLine3, x.townOrCity, x.postCode, x.countryUk))
  )
}
