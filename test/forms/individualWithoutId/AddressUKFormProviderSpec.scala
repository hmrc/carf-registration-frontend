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

import forms.behaviours.StringFieldBehaviours
import models.countries.Country
import play.api.data.FormError

class AddressUKFormProviderSpec extends StringFieldBehaviours {

  private val gb: Country                 = Country("GB", "United Kingdom")
  private val france: Country             = Country("FR", "France")
  private val jersey: Country             = Country("JE", "Jersey")
  private val mockCountries: Seq[Country] = Seq(gb, france, jersey)

  val formProvider = new AddressFormProvider()
  val form         = formProvider(mockCountries)

  // DO MORE TESTS
  ".addressLine1" - {

    val fieldName   = "addressLine1"
    val requiredKey = "address.error.addressLine1.required"
    val lengthKey   = "address.error.addressLine1.length"
    val maxLength   = 35

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }

  ".addressLine2" - {

    val fieldName   = "addressLine2"
    val requiredKey = "address.error.addressLine2.required"
    val lengthKey   = "address.error.addressLine2.length"
    val maxLength   = 35

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
