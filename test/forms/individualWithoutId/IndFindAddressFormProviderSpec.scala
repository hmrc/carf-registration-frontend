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

    val fieldName     = "postcode"
    val requiredKey   = "indFindAddress.error.postcode.required"
    val lengthKey     = "indFindAddress.error.postcode.length"
    val invalidKey    = "indFindAddress.error.postcode.invalid"
    val invalidFormat = "indFindAddress.error.postcode.invalidFormat"
    val maxLength     = 10

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validPostcodes
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
      FormError(fieldName, invalidFormat)
    )

    behave like fieldWithInvalidData(
      form,
      fieldName,
      "!#2",
      FormError(fieldName, invalidKey),
      Some("format")
    )

    "must not bind postcodes longer than 10 characters" in {
      val result = form.bind(Map(fieldName -> "SW1A 1AAAAAAA"))
      result.errors must contain(FormError(fieldName, lengthKey))
    }

    "must bind postcodes with leading, trailing spaces and spaces between characters" in {
      val result = form.bind(Map(fieldName -> "  S W   1A 1AA  "))
      result.errors.filter(_.key == fieldName) mustBe empty
    }
  }

  ".propertyNameOrNumber" - {

    val fieldName = "propertyNameOrNumber"
    val lengthKey = "indFindAddress.error.propertyNameOrNumber.length"
    val maxLength = 35

    "must bind valid data" in {
      forAll(stringsWithMaxLength(maxLength) -> "validString") { string =>
        val result = form.bind(Map("postcode" -> "SW1A 1AA", fieldName -> string))
        result.errors.filter(_.key == fieldName) mustBe empty
      }
    }

    "must bind an empty string as None" in {
      val result = form.bind(Map("postcode" -> "SW1A 1AA", fieldName -> ""))
      result.value.flatMap(_.propertyNameOrNumber) mustBe None
    }

    "must not bind when field is missing" in {
      val result = form.bind(Map("postcode" -> "SW1A 1AA"))
      result.value.flatMap(_.propertyNameOrNumber) mustBe None
    }

    "must not bind strings longer than 35 characters" in {
      forAll(stringsLongerThan(maxLength) -> "longString") { string =>
        val result = form.bind(Map("postcode" -> "SW1A 1AA", fieldName -> string))
        result.errors must contain only FormError(fieldName, lengthKey, Seq(maxLength))
      }
    }

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

  }
}
