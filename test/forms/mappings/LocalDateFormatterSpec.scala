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

import org.scalatest.{EitherValues, OptionValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LocalDateFormatterSpec extends AnyFreeSpec with Matchers with OptionValues with Mappings with EitherValues {

  implicit val messages: Messages = stubMessages()
  val minDate: LocalDate          = LocalDate.of(1900, 1, 1)
  val maxDate: LocalDate          = LocalDate.now().minusDays(1)

  val formatter = new LocalDateFormatter(
    invalidKey = "error.invalid",
    notRealDateKey = "error.notReal",
    allRequiredKey = "error.required.all",
    dayRequiredKey = "error.required.day",
    monthRequiredKey = "error.required.month",
    yearRequiredKey = "error.required.year",
    dayAndMonthRequiredKey = "error.required.dayAndMonth",
    dayAndYearRequiredKey = "error.required.dayAndYear",
    monthAndYearRequiredKey = "error.required.monthAndYear",
    futureDateKey = "error.future",
    pastDateKey = "error.past",
    maxDate = maxDate,
    minDate = minDate
  )

  val validData: Map[String, String] = Map(
    "date.day"   -> "5",
    "date.month" -> "3",
    "date.year"  -> "2020"
  )

  val allFields: Seq[String] = Seq("date.error.day", "date.error.month", "date.error.year")

  "LocalDateFormatter" - {

    "must bind a valid date" in {
      val result = formatter.bind("date", validData)
      result.value mustEqual LocalDate.of(2020, 3, 5)
    }

    "must bind a valid date with textual month" in {
      val result = formatter.bind("date", validData.updated("date.month", "March"))
      result.value mustEqual LocalDate.of(2020, 3, 5)
    }

    "must unbind a date" in {
      val result = formatter.unbind("date", LocalDate.of(2020, 3, 5))
      result mustEqual Map("date.day" -> "5", "date.month" -> "3", "date.year" -> "2020")
    }

    "must correctly bind a leap day" in {
      val leapYearData = Map("date.day" -> "29", "date.month" -> "2", "date.year" -> "2024")
      val result       = formatter.bind("date", leapYearData)
      result.value mustEqual LocalDate.of(2024, 2, 29)
    }

    "must bind the minimum allowed date" in {
      val minDateData = Map(
        "date.day"   -> minDate.getDayOfMonth.toString,
        "date.month" -> minDate.getMonthValue.toString,
        "date.year"  -> minDate.getYear.toString
      )
      val result      = formatter.bind("date", minDateData)
      result.value mustEqual minDate
    }

    "must bind the maximum allowed date" in {
      val maxDateData = Map(
        "date.day"   -> maxDate.getDayOfMonth.toString,
        "date.month" -> maxDate.getMonthValue.toString,
        "date.year"  -> maxDate.getYear.toString
      )
      val result      = formatter.bind("date", maxDateData)
      result.value mustEqual maxDate
    }

    "Error scenarios" - {

      "must fail with a 'required' error (Priority 1)" - {
        "when all fields are empty" in {
          val result = formatter.bind("date", Map.empty[String, String])
          result mustEqual Left(
            Seq(
              FormError("date.day", "error.required.all"),
              FormError("date.month", "error.required.all"),
              FormError("date.year", "error.required.all")
            )
          )
        }

        "when year is missing" in {
          val result = formatter.bind("date", validData - "date.year")
          result mustEqual Left(
            Seq(
              FormError("date.year", "error.required.year")
            )
          )
        }

        "when day and year are missing" in {
          val result = formatter.bind("date", Map("date.month" -> "3"))
          result mustEqual Left(
            Seq(
              FormError("date.day", "error.required.dayAndYear"),
              FormError("date.year", "error.required.dayAndYear")
            )
          )
        }

        "when day is missing" in {
          val result = formatter.bind("date", validData - "date.day")
          result mustEqual Left(
            Seq(
              FormError("date.day", "error.required.day")
            )
          )
        }

        "when month and year are missing" in {
          val result = formatter.bind("date", Map("date.day" -> "21"))
          result mustEqual Left(
            Seq(
              FormError("date.month", "error.required.monthAndYear"),
              FormError("date.year", "error.required.monthAndYear")
            )
          )
        }
      }

      "must fail with an 'invalid' error (Priority 2)" - {
        "when day is not a number" in {
          val result = formatter.bind("date", validData.updated("date.day", "invalid"))
          result mustEqual Left(
            Seq(
              FormError("date.day", "error.invalid")
            )
          )
        }
        "when month is not a valid number or text" in {
          val result = formatter.bind("date", validData.updated("date.month", "invalid"))
          result mustEqual Left(
            Seq(
              FormError("date.month", "error.invalid")
            )
          )
        }
        "when multiple fields are invalid (should highlight all)" in {
          val result = formatter.bind("date", Map("date.day" -> "x", "date.month" -> "y", "date.year" -> "2020"))
          result mustEqual Left(
            Seq(
              FormError("date.day", "error.invalid"),
              FormError("date.month", "error.invalid")
            )
          )
        }
      }

      "must fail with a 'notReal' error (Priority 3)" - {
        "when the date does not exist (e.g. 31st Feb)" in {
          val result = formatter.bind("date", validData.updated("date.day", "31").updated("date.month", "2"))
          result mustEqual Left(
            Seq(
              FormError("date.day", "error.notReal"),
              FormError("date.month", "error.notReal"),
              FormError("date.year", "error.notReal")
            )
          )
        }
        "when only one input is out of bounds (e.g. day=32)" in {
          val result = formatter.bind("date", validData.updated("date.day", "32"))
          result mustEqual Left(
            Seq(
              FormError("date.day", "error.notReal")
            )
          )
        }
        "when multiple inputs are out of bounds (e.g. day=32, month=13)" in {
          val result = formatter.bind("date", validData.updated("date.day", "32").updated("date.month", "13"))
          result mustEqual Left(
            Seq(
              FormError("date.day", "error.notReal"),
              FormError("date.month", "error.notReal")
            )
          )
        }
        "when the date is a leap day in a non-leap year" in {
          val notLeapYearData =
            Map("date.day" -> "29", "date.month" -> "2", "date.year" -> "2023")
          val result          = formatter.bind("date", notLeapYearData)
          result mustEqual Left(
            Seq(
              FormError("date.day", "error.notReal"),
              FormError("date.month", "error.notReal"),
              FormError("date.year", "error.notReal")
            )
          )
        }
      }

      "must fail with a date range error (Priority 3)" - {
        "when the date is in the future" in {
          val futureDate  = maxDate.plusDays(1)
          val data        = Map(
            "date.day"   -> futureDate.getDayOfMonth.toString,
            "date.month" -> futureDate.getMonthValue.toString,
            "date.year"  -> futureDate.getYear.toString
          )
          val result      = formatter.bind("date", data)
          val displayDate = futureDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
          result mustEqual Left(
            Seq(
              FormError("date.day", "error.future", Seq(displayDate)),
              FormError("date.month", "error.future", Seq(displayDate)),
              FormError("date.year", "error.future", Seq(displayDate))
            )
          )
        }

        "when the date is in the past" in {
          val pastDate = minDate.minusDays(1)
          val data     = Map(
            "date.day"   -> pastDate.getDayOfMonth.toString,
            "date.month" -> pastDate.getMonthValue.toString,
            "date.year"  -> pastDate.getYear.toString
          )
          val result   = formatter.bind("date", data)
          result mustEqual Left(
            Seq(
              FormError("date.day", "error.past"),
              FormError("date.month", "error.past"),
              FormError("date.year", "error.past")
            )
          )
        }
      }
    }
  }
}
