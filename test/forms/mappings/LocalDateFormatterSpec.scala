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

import generators.Generators
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LocalDateFormatterSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaCheckPropertyChecks
    with Generators
    with OptionValues
    with Mappings {

  implicit val messages: Messages = stubMessages()
  val minDate                     = LocalDate.of(1901, 1, 1)
  val maxDate                     = LocalDate.of(2099, 12, 31)

  def displayFormat = DateTimeFormatter.ofPattern("d MMMM yyyy")

  def formatter =
    new LocalDateFormatter(
      invalidKey = "invalid",
      notRealDateKey = "notReal",
      allRequiredKey = "required.all",
      dayRequiredKey = "required.day",
      monthRequiredKey = "required.month",
      yearRequiredKey = "required.year",
      dayAndMonthRequiredKey = "required.day.month",
      dayAndYearRequiredKey = "required.day.year",
      monthAndYearRequiredKey = "required.month.year",
      futureDateKey = "error.future",
      pastDateKey = "error.tooEarlyDate",
      maxDate = maxDate,
      minDate = minDate
    )

  "LocalDateFormatter should" - {
    "successfully bind a valid date" in {
      val result = formatter.bind(
        "date",
        Map(
          "date.day"   -> "05",
          "date.month" -> "03",
          "date.year"  -> "2020"
        )
      )
      result.isRight mustBe true
      result.toOption.get mustBe LocalDate.of(2020, 3, 5)
    }

    "remove leading zeroes, spaces and hyphens before parsing" in {
      val result = formatter.bind(
        "date",
        Map(
          "date.day"   -> " 0 5",
          "date.month" -> " 0 3 ",
          "date.year"  -> "  2 0 2 0  "
        )
      )
      result.isRight mustBe true
      result.toOption.get mustBe LocalDate.of(2020, 3, 5)
    }

    "return error when all fields are missing" in {
      val result = formatter.bind("date", Map.empty)
      result mustBe Left(
        Seq(FormError("date", "required.all", Seq("date.error.day", "date.error.month", "date.error.year")))
      )
    }

    "return a day error when only day is missing" in {
      val result = formatter.bind(
        "date",
        Map(
          "date.day"   -> "",
          "date.month" -> "12",
          "date.year"  -> "2020"
        )
      )
      result mustBe Left(Seq(FormError("date", "required.day", Seq("date.error.day"))))
    }

    "return a month error when only month is missing" in {
      val result = formatter.bind(
        "date",
        Map(
          "date.day"   -> "12",
          "date.month" -> "",
          "date.year"  -> "2020"
        )
      )
      result mustBe Left(Seq(FormError("date", "required.month", Seq("date.error.month"))))
    }

    "return a year error when only year is missing" in {
      val result = formatter.bind(
        "date",
        Map(
          "date.day"   -> "12",
          "date.month" -> "12",
          "date.year"  -> ""
        )
      )
      result mustBe Left(Seq(FormError("date", "required.year", Seq("date.error.year"))))
    }

    "return a day and month error when day and month are missing" in {
      val result = formatter.bind(
        "date",
        Map(
          "date.day"   -> "",
          "date.month" -> "",
          "date.year"  -> "2020"
        )
      )
      result mustBe Left(Seq(FormError("date", "required.day.month", Seq("date.error.day", "date.error.month"))))
    }

    "return a day and year error when day and year are missing" in {
      val result = formatter.bind(
        "date",
        Map(
          "date.day"   -> "",
          "date.month" -> "12",
          "date.year"  -> ""
        )
      )
      result mustBe Left(Seq(FormError("date", "required.day.year", Seq("date.error.day", "date.error.year"))))
    }

    "return a month and year error when month and year are missing" in {
      val result = formatter.bind(
        "date",
        Map(
          "date.day"   -> "21",
          "date.month" -> "",
          "date.year"  -> ""
        )
      )
      result mustBe Left(Seq(FormError("date", "required.month.year", Seq("date.error.month", "date.error.year"))))
    }

    "return real-date error when invalid date e.g. 31 Feb" in {
      val result = formatter.bind(
        "date",
        Map("date.day" -> "31", "date.month" -> "02", "date.year" -> "2020")
      )
      result mustBe Left(Seq(FormError("date", "notReal", Seq("date.error.day", "date.error.month", "date.error.year"))))
    }

    "return invalidKey day error when day is non-numeric" in {
      val result = formatter.bind(
        "date",
        Map(
          "date.day"   -> "xx",
          "date.month" -> "03",
          "date.year"  -> "2020"
        )
      )
      result mustBe Left(Seq(FormError("date", "invalid", Seq("date.error.day"))))
    }

    "return invalidKey month error when month is non-numeric" in {
      val result = formatter.bind(
        "date",
        Map(
          "date.day"   -> "3",
          "date.month" -> "z",
          "date.year"  -> "2020"
        )
      )
      result mustBe Left(Seq(FormError("date", "invalid", Seq("date.error.month"))))
    }

    "return invalidKey year error when year is non-numeric" in {
      val result = formatter.bind(
        "date",
        Map(
          "date.day"   -> "3",
          "date.month" -> "12",
          "date.year"  -> "x"
        )
      )
      result mustBe Left(Seq(FormError("date", "invalid", Seq("date.error.year"))))
    }

    "return a day and month error when day and month are non-numeric" in {
      val result = formatter.bind(
        "date",
        Map(
          "date.day"   -> "x",
          "date.month" -> "y",
          "date.year"  -> "2020"
        )
      )
      result mustBe Left(Seq(FormError("date", "invalid", Seq("date.error.day", "date.error.month"))))
    }

    "return a day and year error when day and year are non-numeric" in {
      val result = formatter.bind(
        "date",
        Map(
          "date.day"   -> "x",
          "date.month" -> "12",
          "date.year"  -> "z"
        )
      )
      result mustBe Left(Seq(FormError("date", "invalid", Seq("date.error.day", "date.error.year"))))
    }

    "return a month and year error when month and year are non-numeric" in {
      val result = formatter.bind(
        "date",
        Map(
          "date.day"   -> "21",
          "date.month" -> "y",
          "date.year"  -> "z"
        )
      )
      result mustBe Left(Seq(FormError("date", "invalid", Seq("date.error.month", "date.error.year"))))
    }

    "return futureDateKey when date is after maxDate" in {
      val result = formatter.bind(
        "date",
        Map(
          "date.day"   -> "01",
          "date.month" -> "01",
          "date.year"  -> "3000"
        )
      )
      result mustBe Left(
        Seq(
          FormError(
            "date",
            List("error.future"),
            List(maxDate.format(displayFormat), "date.error.day", "date.error.month", "date.error.year")
          )
        )
      )
    }

    "return pastDateKey when date is before minDate" in {
      val result = formatter.bind(
        "date",
        Map(
          "date.day"   -> "01",
          "date.month" -> "01",
          "date.year"  -> "1899"
        )
      )
      result mustBe Left(Seq( FormError("date", List("error.tooEarlyDate"), List("date.error.day", "date.error.month", "date.error.year"))))
    }

    "handle 3-char alphabetic month names (eg: Jan)" in {
      val result = formatter.bind(
        "date",
        Map(
          "date.day"   -> "5",
          "date.month" -> "Jan",
          "date.year"  -> "2020"
        )
      )
      result.isRight mustBe true
      result.toOption.get mustBe LocalDate.of(2020, 1, 5)
    }

    "handle mixed-case 3-char alphabetic month names (eg: mAr)" in {
      val result = formatter.bind(
        "date",
        Map(
          "date.day"   -> "5",
          "date.month" -> "mAr",
          "date.year"  -> "2000"
        )
      )
      result.isRight mustBe true
      result.toOption.get mustBe LocalDate.of(2000, 3, 5)
    }

    "handle full alphabetic month names (eg: January)" in {
      val result = formatter.bind(
        "date",
        Map(
          "date.day"   -> "31",
          "date.month" -> "January",
          "date.year"  -> "2001"
        )
      )
      result.isRight mustBe true
      result.toOption.get mustBe LocalDate.of(2001, 1, 31)
    }

    "handle mixed-case full alphabetic month names (eg: JaNuaRy)" in {
      val result = formatter.bind(
        "date",
        Map(
          "date.day"   -> "15",
          "date.month" -> "JaNuaRy",
          "date.year"  -> "1999"
        )
      )
      result.isRight mustBe true
      result.toOption.get mustBe LocalDate.of(1999, 1, 15)
    }

    "correctly unbind into day/month/year fields" in {
      val date = LocalDate.of(2020, 7, 15)
      formatter.unbind("date", date) mustBe Map(
        "date.day"   -> "15",
        "date.month" -> "7",
        "date.year"  -> "2020"
      )
    }
  }
}
