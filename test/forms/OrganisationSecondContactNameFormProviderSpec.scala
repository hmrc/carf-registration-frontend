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

class OrganisationSecondContactNameFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "organisationSecondContactName.error.required"
  val lengthKey   = "organisationSecondContactName.error.length"
  val invalidKey  = "organisationSecondContactName.error.invalid"
  val maxLength   = 35

  val form = new OrganisationSecondContactNameFormProvider()()

  val validSecondContactNameChars: Seq[Char] =
    ('a' to 'z') ++
      ('A' to 'Z') ++
      ('0' to '9') ++
      Seq(' ', '&', '\'', '\\', '`', '^', '-')

  val secondContactNameGen: Gen[String] =
    for {
      length <- Gen.choose(1, maxLength)
      chars  <- Gen.listOfN(length, Gen.oneOf(validSecondContactNameChars))
    } yield chars.mkString

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      secondContactNameGen
    )

    behave like fieldWithNonEmptyWhitespace(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
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

    "not bind strings with invalid characters" in {
      val invalidCharGen = Gen.oneOf("!\"#$%()*+,./:;<=>?@[]_{|}~").map(_.toString)
      forAll(invalidCharGen) { invalidChar =>
        val result = form.bind(Map(fieldName -> s"invalid${invalidChar}char")).apply(fieldName)
        result.errors must contain(FormError(fieldName, invalidKey))
      }
    }

    "not bind strings longer than the max length - 35 characters" in {
      val longString = "a" * (maxLength + 1)

      val result = form.bind(Map(fieldName -> longString))
      result.errors must contain only FormError(fieldName, lengthKey, Seq.empty)
    }
  }
}
