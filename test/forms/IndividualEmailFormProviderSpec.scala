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

import config.Constants
import forms.behaviours.StringFieldBehaviours
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class IndividualEmailFormProviderSpec extends StringFieldBehaviours {

  val requiredKey                               = "individualEmail.error.required"
  val lengthKey                                 = "individualEmail.error.length"
  val invalidKey                                = "individualEmail.error.invalid"
  val maxLength                                 = Constants.validEmailMaxLength
  val validEmailAddress132chars64charUsername   =
    "pooYokBZRRdpUsizrOdDbKarOtRjFT.WQOpooYokBZRRdpUsizrOdDbKarOtRjFT@123456789012345678901234567890WQOpooYokBZRRdpUsizrOdDbKarOtRjFT.com"
  val invalidEmailAddress133chars64charUsername =
    "pooYokBZRRdpUsizrOdDbKarOtRjFT.WQOpooYokBZRRdpUsizrOdDbKarOtRjFT@x123456789012345678901234567890WQOpooYokBZRRdpUsizrOdDbKarOtRjFT.com"
  val form: Form[String]                        = new IndividualEmailFormProvider()()

  ".value" - {
    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validEmailAddress132chars64charUsername
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like fieldWithMaxLengthEmail(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like fieldWithInvalidData(
      form,
      fieldName,
      invalidString = "not validemail@ @ test",
      error = FormError(fieldName, invalidKey)
    )

    "not bind invalid email formats" in {
      val invalidEmails = Gen.oneOf(
        invalidEmailAddress133chars64charUsername,
        "testemail",
        "@example.com",
        "@.com",
        "@x.com",
        ":x@x.com",
        ";x@x.com",
        "<x@x.com",
        "@£@x.com",
        "[x@ex.com",
        "]x@ex.com",
        "(x@ex.com",
        ")x@ex.com",
        "test@",
        "test @example.com",
        "test@exam ple.com",
        "test@example.c om",
        "test@@example.com",
        "test@xyz@example.com",
        "test@.com",
        "test@example",
        ".user@example.com",
        "user.@example.com",
        "user..name@example.com",
        "user@example..com",
        "user@example..",
        "user@-example.com",
        "user@example.com-",
        "user@example.com."
      )

      forAll(invalidEmails) { email =>
        val result = form.bind(Map(fieldName -> email))
        result.errors must contain(FormError(fieldName, invalidKey))
      }
    }
  }
}
