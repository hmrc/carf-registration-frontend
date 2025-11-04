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

import config.Constants.individualNameRegex
import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError
import wolfendale.scalacheck.regexp.RegexpGen

class WhatIsYourNameIndividualFormProviderSpec extends StringFieldBehaviours {

  val form = new WhatIsYourNameIndividualFormProvider()()

  ".firstName" - {

    val fieldName   = "firstName"
    val requiredKey = "whatIsYourNameIndividual.error.firstName.required"
    val invalidKey  = "whatIsYourNameIndividual.error.firstName.invalid"
    val lengthKey   = "whatIsYourNameIndividual.error.firstName.length"
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

    "must return an error when firstName is invalid" in {
      val invalidFirstNames: Seq[String] = Seq("John@", "John!", "John@Smith")

      invalidFirstNames.foreach { invalidFirstName =>
        val result = form.bind(
          Map(
            "firstName" -> invalidFirstName,
            "lastName"  -> "Smith"
          )
        )
        result.errors.size mustBe 1
        result.errors.head.message mustBe "whatIsYourNameIndividual.error.firstName.invalid"
      }
    }
  }

  ".lastName" - {

    val fieldName   = "lastName"
    val requiredKey = "whatIsYourNameIndividual.error.lastName.required"
    val invalidKey  = "whatIsYourNameIndividual.error.lastName.invalid"
    val lengthKey   = "whatIsYourNameIndividual.error.lastName.length"
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

    "must return an error when lastName is invalid" in {
      val invalidLastNames: Seq[String] = Seq("Smith@", "Timmey!", "@Smith", "O'Corner#", " Do@e")

      invalidLastNames.foreach { invalidLastName =>
        val result = form.bind(
          Map(
            "firstName" -> "John",
            "lastName"  -> invalidLastName
          )
        )
        result.errors.size mustBe 1
        result.errors.head.message mustBe "whatIsYourNameIndividual.error.lastName.invalid"
      }
    }
  }
}
