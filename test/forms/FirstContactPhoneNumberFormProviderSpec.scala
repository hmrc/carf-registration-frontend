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
import org.scalacheck.Gen
import play.api.data.FormError

class FirstContactPhoneNumberFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "firstContactPhoneNumber.error.required"
  val lengthKey   = "firstContactPhoneNumber.error.length"
  val invalidKey  = "firstContactPhoneNumber.error.invalid"

  val maxLength = 24

  val form = new FirstContactPhoneNumberFormProvider()()

  val validNumbers = Seq(
    "07123456789",
    "+447123456789",
    "02079460000",
    "+1 650 253 0000",
    "+33 1 42 68 53 00",
    "+49 30 123456",
    "+91 98765 43210",
    "07400111222 ext 5",
    "++447123456789", // google lib tries to recover extra punctuation where possible, like parsing ++44 as +44
    "+1 (650) 253-0000 x123"
  )

  val invalidNumbers = Seq(
    "abcdefg",
    "12345",
    "+999999999",
    "+44",
    "071234567890", // too long
    "0712345678", // too short
    "+44 123"
  )

  ".value" - {

    val fieldName = "value"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "not bind strings with invalid characters" in {
      invalidNumbers.foreach { invalidPhoneNumber =>
        val result = form.bind(Map(fieldName -> invalidPhoneNumber)).apply(fieldName)

        withClue(s"Expected error for invalid phone number: '$invalidPhoneNumber'") {
          result.errors must contain(FormError(fieldName, invalidKey))
        }
      }
    }

    "bind valid phone numbers" in {
      validNumbers.foreach { validPhoneNumber =>
        val testValue = validPhoneNumber
        val result    = form.bind(Map(fieldName -> testValue)).apply(fieldName)

        withClue(s"Expected no errors for valid phone number: '$testValue'") {
          result.errors.isEmpty mustBe true
        }
      }
    }

    "not bind strings longer than the max length" in {
      val longString = "a" * (maxLength + 1)

      val result = form.bind(Map(fieldName -> longString))
      result.errors must contain only FormError(fieldName, lengthKey, Seq.empty)
    }
  }
}
