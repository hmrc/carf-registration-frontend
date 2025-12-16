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

import config.Constants.individualNameRegex
import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError
import wolfendale.scalacheck.regexp.RegexpGen

class IndWithoutNinoNameFormProviderSpec extends StringFieldBehaviours {

  val form = new IndWithoutNinoNameFormProvider()()

  ".givenName" - {
    val fieldName   = "givenName"
    val requiredKey = "indWithoutNinoName.error.givenName.required"
    val invalidKey  = "indWithoutNinoName.error.givenName.invalid"
    val lengthKey   = "indWithoutNinoName.error.givenName.length"
    val maxLength   = 35

    behave like fieldThatBindsValidDataWithoutInvalidError(
      form,
      fieldName,
      RegexpGen.from(individualNameRegex),
      invalidKey
    )

    behave like fieldWithMaxLengthAlphanumeric(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq())
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must return an error when givenName is invalid" in {
      val invalidFirstNames: Seq[String] = Seq("John@", "John!", "John@Smith")

      invalidFirstNames.foreach { invalidFirstName =>
        val result = form.bind(
          Map(
            "givenName"  -> invalidFirstName,
            "familyName" -> "Smith"
          )
        )
        result.errors.size mustBe 1
        result.errors.head.message mustBe "indWithoutNinoName.error.givenName.invalid"
      }
    }
  }

  ".familyName" - {
    val fieldName   = "familyName"
    val requiredKey = "indWithoutNinoName.error.familyName.required"
    val invalidKey  = "indWithoutNinoName.error.familyName.invalid"
    val lengthKey   = "indWithoutNinoName.error.familyName.length"
    val maxLength   = 35

    behave like fieldThatBindsValidDataWithoutInvalidError(
      form,
      fieldName,
      RegexpGen.from(individualNameRegex),
      invalidKey
    )

    behave like fieldWithMaxLengthAlphanumeric(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq())
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must return an error when familyName is invalid" in {
      val invalidLastNames: Seq[String] = Seq("Smith@", "Timmey!", "@Smith", "O'Corner#", " Do@e")

      invalidLastNames.foreach { invalidLastName =>
        val result = form.bind(
          Map(
            "givenName"  -> "John",
            "familyName" -> invalidLastName
          )
        )
        result.errors.size mustBe 1
        result.errors.head.message mustBe "indWithoutNinoName.error.familyName.invalid"
      }
    }
  }
}
