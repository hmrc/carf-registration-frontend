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

package forms.behaviours

import forms.FormSpec
import generators.Generators
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.{Form, FormError}

trait FieldBehaviours extends FormSpec with ScalaCheckPropertyChecks with Generators {

  def fieldThatBindsValidData(form: Form[_], fieldName: String, validDataGenerator: Gen[String]): Unit =
    "bind valid data" in {

      forAll(validDataGenerator -> "validDataItem") { (dataItem: String) =>
        val result = form.bind(Map(fieldName -> dataItem)).apply(fieldName)
        result.value.value mustBe dataItem
        result.errors      mustBe empty
      }
    }

  def mandatoryField(form: Form[_], fieldName: String, requiredError: FormError): Unit = {

    "not bind when key is not present at all" in {

      val result = form.bind(emptyForm).apply(fieldName)
      result.errors mustEqual Seq(requiredError)
    }

    "not bind blank values" in {

      val result = form.bind(Map(fieldName -> "")).apply(fieldName)
      result.errors mustEqual Seq(requiredError)
    }
  }

  def fieldWithNonEmptyWhitespace(form: Form[_], fieldName: String, requiredError: FormError): Unit =
    s"must not bind strings of only whitespace" in {

      val result = form.bind(Map(fieldName -> " ")).apply(fieldName)
      result.errors mustEqual Seq(requiredError)
    }

  def fieldWithInvalidData(
      form: Form[_],
      fieldName: String,
      invalidString: String,
      error: FormError,
      suffix: Option[String] = None
  ): Unit = {
    val testName = if (suffix.isEmpty) "not bind invalid data" else s"not bind invalid data ${suffix.get}"
    testName in {
      val result = form.bind(Map(fieldName -> invalidString)).apply(fieldName)
      result.errors mustEqual Seq(error)
    }
  }

  def generateValidStringLongerThan(maxLength: Int, allowedChars: String): Gen[String] = for {
    length <- Gen.chooseNum(maxLength + 1, maxLength * 2)
    chars  <- Gen.listOfN(length, Gen.oneOf(allowedChars))
  } yield chars.mkString

  def fieldWithValidCharsLongerThanMaxLength(
      form: Form[_],
      fieldName: String,
      validCharacters: String,
      maxLength: Int,
      lengthError: FormError
  ): Unit =
    s"not bind strings longer than $maxLength which have valid characters" in {
      forAll(generateValidStringLongerThan(maxLength, validCharacters) -> "longValidString") { (string: String) =>
        val result = form.bind(Map(fieldName -> string)).apply(fieldName)
        result.errors must contain only lengthError
      }
    }
}
