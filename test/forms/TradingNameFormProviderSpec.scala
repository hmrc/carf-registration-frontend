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
import play.api.data.FormError
import org.scalacheck.Gen

class TradingNameFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "tradingName.error.required"
  val lengthKey   = "tradingName.error.length"
  val invalidKey  = "tradingName.error.invalid"
  val maxLength   = 80

  val form = new TradingNameFormProvider()()

  val validTradingNameChars: Gen[Char] = Gen.oneOf(
    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 &'\\`^-".toSeq
  )

  val tradingNameGen: Gen[String] =
    for {
      length <- Gen.choose(1, maxLength)
      chars  <- Gen.listOfN(length, validTradingNameChars)
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
      val invalidCharGen = Gen.oneOf("!@£$%_=+<>,./?~#").map(_.toString)
      forAll(invalidCharGen) { invalidChar =>
        val result = form.bind(Map(fieldName -> s"invalid${invalidChar}char")).apply(fieldName)
        result.errors must contain(FormError(fieldName, invalidKey))
      }
    }

    "not bind strings longer than the max length" in {
      val longString = "a" * (maxLength + 1)

      val result = form.bind(Map(fieldName -> longString))
      result.errors must contain only FormError(fieldName, lengthKey, Seq(maxLength))
    }
  }
}
