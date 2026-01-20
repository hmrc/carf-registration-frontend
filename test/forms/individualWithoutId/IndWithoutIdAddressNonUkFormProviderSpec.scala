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
import forms.individualWithoutId.IndWithoutIdAddressNonUkFormProvider
import models.Country
import play.api.data.FormError

class IndWithoutIdAddressNonUkFormProviderSpec extends StringFieldBehaviours {

  val countries = Seq(Country("FR", "France"), Country("DE", "Germany"))
  val form      = new IndWithoutIdAddressNonUkFormProvider()(countries)

  ".addressLine1" - {

    val fieldName   = "addressLine1"
    val requiredKey = "indWithoutIdAddressNonUk.addressLine1.error.required"
    val lengthKey   = "indWithoutIdAddressNonUk.addressLine1.error.length"
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

    val fieldName = "addressLine2"
    val lengthKey = "indWithoutIdAddressNonUk.addressLine2.error.length"
    val maxLength = 35

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

    behave like optionalField(
      form,
      fieldName
    )
  }

  ".townOrCity" - {

    val fieldName   = "townOrCity"
    val requiredKey = "indWithoutIdAddressNonUk.townOrCity.error.required"
    val lengthKey   = "indWithoutIdAddressNonUk.townOrCity.error.length"
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

  ".region" - {

    val fieldName = "region"
    val lengthKey = "indWithoutIdAddressNonUk.region.error.length"
    val maxLength = 35

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

    behave like optionalField(
      form,
      fieldName
    )
  }

  ".country" - {

    val fieldName   = "country"
    val requiredKey = "indWithoutIdAddressNonUk.country.error.required"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      countries.map(_.code)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must not bind invalid country codes" in {
      val result = form.bind(Map(fieldName -> "INVALID")).apply(fieldName)
      result.errors mustEqual Seq(FormError(fieldName, requiredKey, Seq.empty))
    }
  }
}
