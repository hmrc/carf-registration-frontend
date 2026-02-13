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

import config.Constants.{addressMaxLength, addressRegex}
import forms.mappings.Mappings
import models.IndWithoutIdAddressNonUk
import play.api.data.Form
import play.api.data.Forms._

import javax.inject.Inject
import models.countries.Country

class IndWithoutIdAddressNonUkFormProvider @Inject() extends Mappings {

  def apply(countryList: Seq[Country]): Form[IndWithoutIdAddressNonUk] = Form(
    mapping(
      "addressLine1" -> validatedText(
        requiredKey = "indWithoutIdAddressNonUk.addressLine1.error.required",
        lengthKey = "indWithoutIdAddressNonUk.addressLine1.error.length",
        invalidKey = "indWithoutIdAddressNonUk.addressLine1.error.invalid",
        maxLength = addressMaxLength,
        regex = addressRegex
      ),
      "addressLine2" -> validatedOptionalText(
        lengthKey = "indWithoutIdAddressNonUk.addressLine2.error.length",
        invalidKey = "indWithoutIdAddressNonUk.addressLine2.error.invalid",
        maxLength = addressMaxLength,
        regex = addressRegex
      ),
      "townOrCity"   -> validatedText(
        requiredKey = "indWithoutIdAddressNonUk.townOrCity.error.required",
        lengthKey = "indWithoutIdAddressNonUk.townOrCity.error.length",
        invalidKey = "indWithoutIdAddressNonUk.townOrCity.error.invalid",
        maxLength = addressMaxLength,
        regex = addressRegex
      ),
      "region"       -> validatedOptionalText(
        lengthKey = "indWithoutIdAddressNonUk.region.error.length",
        invalidKey = "indWithoutIdAddressNonUk.region.error.invalid",
        maxLength = addressMaxLength,
        regex = addressRegex
      ),
      "postcode"     -> validatedOptionalText(
        lengthKey = "indWithoutIdAddressNonUk.postcode.error.length",
        invalidKey = "indWithoutIdAddressNonUk.postcode.error.invalid",
        maxLength = 10,
        regex = "^[A-Za-z0-9 ]*$"
      ),
      "country"      -> text("indWithoutIdAddressNonUk.country.error.required")
        .verifying("indWithoutIdAddressNonUk.country.error.required", value => countryList.exists(_.code == value))
        .transform[Country](
          value =>
            countryList
              .find(_.code.contains(value))
              .getOrElse(throw new IllegalStateException(s"Failed to derive country given code [$value]")),
          country => country.code
        )
    )(IndWithoutIdAddressNonUk.apply)(x =>
      Some((x.addressLine1, x.addressLine2, x.townOrCity, x.region, x.postcode, x.country))
    )
  )
}
