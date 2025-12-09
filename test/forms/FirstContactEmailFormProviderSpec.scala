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

import forms.behaviours.StringFieldBehaviours
import forms.organisation.FirstContactEmailFormProvider
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class FirstContactEmailFormProviderSpec extends StringFieldBehaviours {

  val requiredKey: String = "firstContactEmail.error.required"
  val lengthKey: String   = "firstContactEmail.error.length"
  val invalidKey: String  = "firstContactEmail.error.invalid"
  val maxLength: Int      = 132
  val validEmailAddress   = "avalid@email.com"

  val form: Form[String] = new FirstContactEmailFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validEmailAddress
    )

    behave like fieldWithInvalidData(
      form,
      fieldName,
      invalidString = "not validemail@ @ test",
      error = FormError(fieldName, invalidKey)
    )

    behave like fieldWithMaxLengthEmail(
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

    "not bind invalid email formats" in {
      val invalidEmails = Gen.oneOf(
        "testemail",
        "@example.com",
        "test@",
        "test @example.com",
        "test@exam ple.com",
        "test@@example.com",
        "test@.com",
        "test@example",
        ".user@example.com",
        "user.@example.com",
        "user..name@example.com",
        "user@example..com",
        "user@-example.com",
        "user@example.com-"
      )

      forAll(invalidEmails) { email =>
        val result = form.bind(Map(fieldName -> email))
        result.errors must contain(FormError(fieldName, invalidKey))
      }
    }

  }
}
