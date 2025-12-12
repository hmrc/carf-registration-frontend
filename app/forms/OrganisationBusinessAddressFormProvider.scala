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

import javax.inject.Inject
import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.Forms.*
import models.{Country, OrganisationBusinessAddress}

class OrganisationBusinessAddressFormProvider @Inject() extends Mappings {
  
  def apply(countryList: Seq[Country]): Form[OrganisationBusinessAddress] = Form(
    mapping(
      "AddressLine1" -> text("organisationBusinessAddress.error.AddressLine1.required")
        .verifying(maxLength(35, "organisationBusinessAddress.error.AddressLine1.length")),
      "AddressLine2" -> text("organisationBusinessAddress.error.AddressLine2.required")
        .verifying(maxLength(35, "organisationBusinessAddress.error.AddressLine2.length")),
      "country"      -> text("addressWithoutId.error.country.required")
        .verifying(
          "addressWithoutId.error.country.required",
          value => countryList.exists(_.code == value)
        )
        .transform[Country](
          value =>
            countryList
              .find(_.code.contains(value))
              .getOrElse(throw new IllegalStateException(s"Failed to derive country given code [$value]")),
          country => country.code
        )
    )(OrganisationBusinessAddress.apply)(x => Some((x.AddressLine1, x.AddressLine2, x.country)))
  )
}
