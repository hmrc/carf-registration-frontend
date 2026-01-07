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

package forms.individualWithoutId

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class IndFindAddressFormProviderSpec extends StringFieldBehaviours {

  val form = new IndFindAddressFormProvider()()

  ".postcode" - {

    val fieldName      = "postcode"
    val requiredKey    = "indFindAddress.error.postcode.required"
    val lengthKey      = "indFindAddress.error.postcode.length"
    val invalidKey     = "indFindAddress.error.postcode.invalid"
    val invalidCharKey = "indFindAddress.error.postcode.chars"
    val maxLength      = 10

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      postcodeStringGen
    )

    behave like fieldWithMaxLengthAlphanumeric(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like fieldWithInvalidData(
      form,
      fieldName,
      "xx9 9xx9",
      FormError(fieldName, invalidKey)
    )

    behave like fieldWithInvalidData(
      form,
      fieldName,
      "!#2",
      FormError(fieldName, invalidCharKey),
      Some("chars")
    )
  }

  ".propertyNameOrNumber" - {

    val fieldName   = "propertyNameOrNumber"
    val requiredKey = "indFindAddress.error.propertyNameOrNumber"
    val lengthKey   = "indFindAddress.error.propertyNameOrNumber"
    val maxLength   = 100

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
