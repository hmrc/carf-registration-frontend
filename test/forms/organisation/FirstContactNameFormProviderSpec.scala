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

package forms.organisation

import forms.behaviours.StringFieldBehaviours
import forms.organisation.FirstContactNameFormProvider
import org.scalacheck.Gen
import play.api.data.FormError

class FirstContactNameFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "firstContactName.error.required"
  val lengthKey   = "firstContactName.error.length"
  val invalidKey  = "firstContactName.error.invalid"
  val maxLength   = 35

  val form = new FirstContactNameFormProvider()()

  val validTradingNameChars: Seq[Char] =
    ('a' to 'z') ++
      ('A' to 'Z') ++
      ('0' to '9') ++
      Seq(' ', '&', '\'', '\\', '`', '^', '-')

  val tradingNameGen: Gen[String] =
    for {
      length <- Gen.choose(1, maxLength)
      chars  <- Gen.listOfN(length, Gen.oneOf(validTradingNameChars))
    } yield chars.mkString

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      tradingNameGen
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

    "not bind strings longer than the max length" in {
      val longString = "a" * (maxLength + 1)

      val result = form.bind(Map(fieldName -> longString))
      result.errors must contain only FormError(fieldName, lengthKey, Seq.empty)
    }
  }
}
