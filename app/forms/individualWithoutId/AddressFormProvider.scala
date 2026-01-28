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

import config.Constants.addressRegex
import forms.mappings.Mappings
import models.AddressUK
import models.countries.*
import play.api.data.Form
import play.api.data.Forms.*

import javax.inject.Inject

class AddressFormProvider @Inject() extends Mappings {

  inline val maxLength = 35

  def apply(countryList: => Seq[Country]): Form[AddressUK] = Form(
    mapping(
      "addressLine1" -> text("address.addressLine1.error.required").verifying(
        maxLength(35, "address.error.addressLine1.length"),
        regexp(addressRegex, "address.error.addressLine1.invalid")
      ),
      "addressLine2" -> optional(
        text("address.error.addressLine2.required")
          .verifying(
            maxLength(35, "address.error.addressLine2.length"),
            regexp(addressRegex, "address.error.addressLine2.invalid")
          )
      ),
      "townOrCity"   -> text("address.error.townOrCity.required").verifying(
        maxLength(35, "address.error.townOrCity.length"),
        regexp(addressRegex, "address.error.townOrCity.invalid")
      ),
      "county"       -> optional(
        text("address.error.county.required")
          .verifying(
            maxLength(35, "address.error.county.length"),
            regexp(addressRegex, "address.error.county.invalid")
          )
      ),
      "postcode"     -> postcodeNoneEmpty(
        countryList = countryList,
        lengthKey = "address.postcode.error.length",
        invalidCharKey = "address.postcode.error.invalid",
        requiredCrownKey = "address.postcode.error.required",
        invalidFormatCrownKey = "address.postcode.error.invalidFormat",
        invalidRealCrownKey = "address.postcode.error.notReal"
      ),
      "country"      -> text("address.country.error.required")
    )(AddressUK.apply)(x => Some(x.addressLine1, x.addressLine2, x.townOrCity, x.county, x.postCode, x.postCode))
  )
}
