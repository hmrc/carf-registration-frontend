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

package forms.mappings

import models.{Enumerable, UniqueTaxpayerReference}
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.data.{Form, FormError}
import play.api.data.Forms.mapping

object MappingsSpec {

  sealed trait Foo
  case object Bar extends Foo
  case object Baz extends Foo

  object Foo {

    val values: Set[Foo] = Set(Bar, Baz)

    implicit val fooEnumerable: Enumerable[Foo] =
      Enumerable(values.toSeq.map(v => v.toString -> v): _*)
  }
}

class MappingsSpec extends AnyFreeSpec with Matchers with OptionValues with Mappings {

  import MappingsSpec._

  "text" - {

    val testForm: Form[String] =
      Form(
        "value" -> text()
      )

    "must bind a valid string" in {
      val result = testForm.bind(Map("value" -> "foobar"))
      result.get mustEqual "foobar"
    }

    "must not bind an empty string" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind a string of whitespace only" in {
      val result = testForm.bind(Map("value" -> " \t"))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must return a custom error message" in {
      val form   = Form("value" -> text("custom.error"))
      val result = form.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "custom.error"))
    }

    "must unbind a valid value" in {
      val result = testForm.fill("foobar")
      result.apply("value").value.value mustEqual "foobar"
    }
  }

  "boolean" - {

    val testForm: Form[Boolean] =
      Form(
        "value" -> boolean()
      )

    "must bind true" in {
      val result = testForm.bind(Map("value" -> "true"))
      result.get mustEqual true
    }

    "must bind false" in {
      val result = testForm.bind(Map("value" -> "false"))
      result.get mustEqual false
    }

    "must not bind a non-boolean" in {
      val result = testForm.bind(Map("value" -> "not a boolean"))
      result.errors must contain(FormError("value", "error.boolean"))
    }

    "must not bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must unbind" in {
      val result = testForm.fill(true)
      result.apply("value").value.value mustEqual "true"
    }
  }

  "int" - {

    val testForm: Form[Int] =
      Form(
        "value" -> int()
      )

    "must bind a valid integer" in {
      val result = testForm.bind(Map("value" -> "1"))
      result.get mustEqual 1
    }

    "must not bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must unbind a valid value" in {
      val result = testForm.fill(123)
      result.apply("value").value.value mustEqual "123"
    }
  }

  "enumerable" - {

    val testForm = Form(
      "value" -> enumerable[Foo]()
    )

    "must bind a valid option" in {
      val result = testForm.bind(Map("value" -> "Bar"))
      result.get mustEqual Bar
    }

    "must not bind an invalid option" in {
      val result = testForm.bind(Map("value" -> "Not Bar"))
      result.errors must contain(FormError("value", "error.invalid"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }
  }

  "currency" - {

    val testForm: Form[BigDecimal] =
      Form(
        "value" -> currency()
      )

    "must bind a valid integer" in {
      val result = testForm.bind(Map("value" -> "1"))
      result.get mustEqual 1
    }

    "must bind a valid decimal with 1 decimal place" in {
      val result = testForm.bind(Map("value" -> "1.2"))
      result.get mustEqual 1.2
    }

    "must bind a valid decimal with 2 decimal places" in {
      val result = testForm.bind(Map("value" -> "1.23"))
      result.get mustEqual 1.23
    }

    "must bind a valid number with spaces, commas and `£` characters" in {
      val result = testForm.bind(Map("value" -> "£ 1,234 . 01"))
      result.get mustEqual 1234.01
    }

    "must not bind values with a `£` after any numbers" in {
      val result = testForm.bind(Map("value" -> "123 £456"))
      result.errors must contain only FormError("value", "error.nonNumeric")
    }

    "must not bind values with non-numeric characters except commas, spaces and `£`s" in {
      val result = testForm.bind(Map("value" -> "abc"))
      result.errors must contain only FormError("value", "error.nonNumeric")
    }

    "must not bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind a number with more than 2 decimal places" in {
      val result = testForm.bind(Map("value" -> "1.234"))
      result.errors must contain only FormError("value", "error.invalidNumeric")
    }

    "must not bind negative numbers" in {
      val result = testForm.bind(Map("value" -> "-1"))
      result.errors must contain only FormError("value", "error.nonNumeric")
    }

    "must unbind a valid value" in {
      val result = testForm.fill(1)
      result.apply("value").value.value mustEqual "1"
    }
  }

  "validatedUTR" - {
    val testForm: Form[UniqueTaxpayerReference] = Form(
      mapping(
        "value" -> validatedUTR(
          requiredKey = "error.required",
          invalidKey = "error.invalid.key",
          invalidFormatKey = "error.invalid.formatKey",
          regex = "^[0-9]*$",
          msgArg = "UTR"
        )
      )(UniqueTaxpayerReference.apply)(utr => Some(utr.uniqueTaxPayerReference))
    )

    "must bind a valid UTR that is 10 characters long" in {
      val result = testForm.bind(Map("value" -> "1234267890"))
      result.get mustBe UniqueTaxpayerReference("1234267890")
    }

    "must bind a valid UTR that is 13 characters long" in {
      val result = testForm.bind(Map("value" -> "1234267890121"))
      result.get mustBe UniqueTaxpayerReference("1234267890121")
    }

    "must bind a valid UTR that contains spaces" in {
      val result = testForm.bind(Map("value" -> "1 2 3 4 5 6 7 8 9 0"))
      result.get mustBe UniqueTaxpayerReference("1234567890")
    }

    "must bind a UTR with K in the start and whitespace characters" in {
      val result = testForm.bind(Map("value" -> "K123456 789 0"))
      result.get mustBe UniqueTaxpayerReference("1234567890")
    }

    "must remove any 'K' character from the start of the string" in {
      val result = testForm.bind(Map("value" -> "K1234561289"))
      result.get mustBe UniqueTaxpayerReference("1234561289")
    }

    "must remove any 'K' character from the end of the string" in {
      val result = testForm.bind(Map("value" -> "1234561289K"))
      result.get mustBe UniqueTaxpayerReference("1234561289")
    }

    "must not bind an empty string" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required", List("UTR")))
    }

    "must not bind whitespace and k characters only" in {
      val result = testForm.bind(Map("value" -> "k k K K"))
      result.errors must contain(FormError("value", "error.required", List("UTR")))
    }

    "must not bind an invalid UTR (short)" in {
      val result = testForm.bind(Map("value" -> "123456"))
      result.errors must contain(FormError("value", "error.invalid.formatKey", List("UTR")))
    }

    "must not bind a very long invalid UTR " in {
      val result = testForm.bind(Map("value" -> "1234561234567890"))
      result.errors must contain(FormError("value", "error.invalid.formatKey", List("UTR")))
    }

    "must not bind invalid characters" in {
      val result = testForm.bind(Map("value" -> "ABC123!@#$"))
      result.errors must contain(FormError("value", "error.invalid.key", List("UTR")))
    }

    "must not bind a UTR with Kk characters in between and spaces" in {
      val result = testForm.bind(Map("value" -> "12k 34K 567890"))
      result.errors must contain(FormError("value", "error.invalid.key", List("UTR")))
    }

  }

}
