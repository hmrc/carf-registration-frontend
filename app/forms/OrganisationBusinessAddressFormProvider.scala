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

package forms

import config.Constants.{addressMaxLength, addressRegex, crownDependencyPostcodeRegex, postcodeMaxLength, postcodeRegex}
import forms.mappings.Mappings
import models.{Country, OrganisationBusinessAddress}
import play.api.data.Form
import play.api.data.Forms.*
import javax.inject.Inject

class OrganisationBusinessAddressFormProvider @Inject() extends Mappings {

  private val crownDependencies = Seq("GG", "JE", "IM")

  def apply(countryList: Seq[Country]): Form[OrganisationBusinessAddress] = Form(
    mapping(
      "addressLine1" -> validatedText(
        requiredKey = "organisationBusinessAddress.addressLine1.error.required",
        lengthKey = "organisationBusinessAddress.addressLine1.error.length",
        invalidKey = "organisationBusinessAddress.addressLine1.error.invalid",
        maxLength = addressMaxLength,
        regex = addressRegex
      ),
      "addressLine2" -> optional(
        text()
          .verifying(maxLength(addressMaxLength, "organisationBusinessAddress.addressLine2.error.length"))
          .verifying(regexp(addressRegex, "organisationBusinessAddress.addressLine2.error.invalid"))
      ),
      "townOrCity"   -> validatedText(
        requiredKey = "organisationBusinessAddress.townOrCity.error.required",
        lengthKey = "organisationBusinessAddress.townOrCity.error.length",
        invalidKey = "organisationBusinessAddress.townOrCity.error.invalid",
        maxLength = addressMaxLength,
        regex = addressRegex
      ),
      "region"       -> optional(
        text()
          .verifying(maxLength(addressMaxLength, "organisationBusinessAddress.region.error.length"))
          .verifying(regexp(addressRegex, "organisationBusinessAddress.region.error.invalid"))
      ),
      "postcode"     -> optional(
        text()
          .verifying(maxLength(postcodeMaxLength, "organisationBusinessAddress.postcode.error.length"))
          .verifying(regexp(postcodeRegex, "organisationBusinessAddress.postcode.error.invalid"))
      ),
      "country"      -> text("organisationBusinessAddress.country.error.required")
        .verifying("organisationBusinessAddress.country.error.required", code => countryList.exists(_.code == code))
        .transform[Country](
          code => countryList.find(_.code == code).get,
          country => country.code
        )
    )(OrganisationBusinessAddress.apply)(x =>
      Some((x.addressLine1, x.addressLine2, x.townOrCity, x.region, x.postcode, x.country))
    )
      .verifying(
        "organisationBusinessAddress.postcode.error.emptyAndCountryIsJersey",
        address => !crownDependencies.contains(address.country.code) || address.postcode.exists(_.trim.nonEmpty)
      )
      .verifying(
        "organisationBusinessAddress.postcode.error.invalidFormat",
        address =>
          if (crownDependencies.contains(address.country.code) && address.postcode.exists(_.trim.nonEmpty)) {
            address.postcode.get.toUpperCase.matches(crownDependencyPostcodeRegex)
          } else {
            true
          }
      )
  )
}
