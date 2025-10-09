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
import play.api.data.{Form, FormError}

class BusinessWithoutIdBusinessNameFormProviderSpec extends StringFieldBehaviours {
  val requiredKey = "businessWithoutIdBusinessName.error.required"
  val lengthKey   = "businessWithoutIdBusinessName.error.maximumLength"
  val invalidFormatKey = "businessWithoutIdBusinessName.error.invalidFormat"
  val maxLength   = 105
  val validBusinessName = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ -&'^`\\1234567890"
  val form = new BusinessWithoutIdBusinessNameFormProvider()()

  val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789&'\\^`- "
  val businessNameCharacterGenerator: Gen[Char] = Gen.oneOf(allowedChars)

  def validStringLongerThan(minLength: Int): Gen[String] = for {
    maxLength <- Gen.const((minLength * 2).max(maxLength))
    length <- Gen.chooseNum(minLength + 1, maxLength)
    chars <- Gen.listOfN(length, businessNameCharacterGenerator)
  } yield chars.mkString

  def businessNameFieldWithMaxLength(form: Form[_], fieldName: String, maxLength: Int, lengthError: FormError): Unit =
    s"not bind strings longer than $maxLength characters (and valid per regex)" in {
      forAll(validStringLongerThan(maxLength) -> "longValidString") { (string: String) =>
        val result = form.bind(Map(fieldName -> string)).apply(fieldName)
        println(s"result.errors=$result.errors")
        result.errors must contain only lengthError
      }
    }

  ".value" - {
    val fieldName = "value"
    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator = validBusinessName
    )
    behave like businessNameFieldWithMaxLength(
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
      "A@$%^&a![]{}*",
      FormError(fieldName, invalidFormatKey)
    )
  }
}
