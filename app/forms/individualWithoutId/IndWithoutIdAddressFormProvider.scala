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

import config.Constants.{addressMaxLength, addressRegex, postCodeAllowedChars, regexPostcode}
import forms.mappings.Mappings
import models.AddressUk
import models.countries.{Country, CountryUk}
import play.api.data.Form
import play.api.data.Forms.*

import javax.inject.Inject

class IndWithoutIdAddressFormProvider @Inject() extends Mappings {

  def apply(countryList: Seq[Country]): Form[AddressUk] = Form(
    mapping(
      "addressLine1" -> text("address.addressLine1.error.required").verifying(
        firstError(
          maxLength(addressMaxLength, "address.addressLine1.error.length"),
          regexp(addressRegex, "address.addressLine1.error.invalid")
        )
      ),
      "addressLine2" -> validatedOptionalText(
        lengthKey = "address.addressLine2.error.length",
        invalidKey = "address.addressLine2.error.invalid",
        maxLength = addressMaxLength,
        regex = addressRegex
      ),
      "addressLine3" -> validatedOptionalText(
        lengthKey = "address.addressLine3.error.length",
        invalidKey = "address.addressLine3.error.invalid",
        maxLength = addressMaxLength,
        regex = addressRegex
      ),
      "townOrCity"   -> text("address.townOrCity.error.required").verifying(
        firstError(
          maxLength(addressMaxLength, "address.townOrCity.error.length"),
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
