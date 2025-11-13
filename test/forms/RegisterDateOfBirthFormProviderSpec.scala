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

import java.time.{LocalDate, ZoneOffset}
import java.time.format.DateTimeFormatter
import forms.behaviours.DateBehaviours
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import play.api.data.FormError

class RegisterDateOfBirthFormProviderSpec extends DateBehaviours {
  private implicit val messages: Messages = stubMessages()
  private val form                        = new RegisterDateOfBirthFormProvider()()

  private val minDate      = LocalDate.of(1900, 1, 1)
  private val today        = LocalDate.now(ZoneOffset.UTC)
  private val maxValidDate = today.minusDays(1)
  val displayFormat        = DateTimeFormatter.ofPattern("d MMMM yyyy")

  ".value" - {
    val validData = datesBetween(min = minDate, max = maxValidDate)
    behave like dateField(form, "value", validData)
    behave like mandatoryDateField(
      form,
      "value",
      "registerDateOfBirth.error.required.all",
      List("date.error.day", "date.error.month", "date.error.year")
    )

    "must bind the minimum allowed date (1900-01-01)" in {
      val data   = Map(
        "value.day"   -> "1",
        "value.month" -> "1",
        "value.year"  -> "1900"
      )
      val result = form.bind(data)
      result.value.value mustEqual LocalDate.of(1900, 1, 1)
    }

    "must bind the maximum allowed date (today’s date minus 1 day)" in {
      val data   = Map(
        "value.day"   -> maxValidDate.getDayOfMonth.toString,
        "value.month" -> maxValidDate.getMonthValue.toString,
        "value.year"  -> maxValidDate.getYear.toString
      )
      val result = form.bind(data)
      result.value.value mustEqual maxValidDate
    }

    "must bind leap year date 29 February 2000" in {
      val data   = Map(
        "value.day"   -> "29",
        "value.month" -> "2",
        "value.year"  -> "2000"
      )
      val result = form.bind(data)
      result.value.value mustEqual LocalDate.of(2000, 2, 29)
    }

    "must bind leap year date 29 February 2020" in {
      val data   = Map(
        "value.day"   -> "29",
        "value.month" -> "2",
        "value.year"  -> "2020"
      )
      val result = form.bind(data)
      result.value.value mustEqual LocalDate.of(2020, 2, 29)
    }

    "must bind valid date with leading zeros (e.g. 05/09/1995)" in {
      val data   = Map(
        "value.day"   -> "05",
        "value.month" -> "09",
        "value.year"  -> "1995"
      )
      val result = form.bind(data)
      result.value.value mustEqual LocalDate.of(1995, 9, 5)
    }

    "must bind valid dates when month provided by alphabetic name (case-insensitive)" in {
      val date       = LocalDate.of(2000, 3, 15)
      val monthNames = Seq("March", "march", "MARCH", "Mar", "mar", "MAR")

      monthNames.foreach { m =>
        val data   = Map(
          "value.day"   -> date.getDayOfMonth.toString,
          "value.month" -> m,
          "value.year"  -> date.getYear.toString
        )
        val result = form.bind(data)
        result.value.value mustEqual date
      }
    }

    "must bind valid dates with month names in random mixed case" in {
      def toMixedCase(s: String): String =
        s.map(c => if (scala.util.Random.nextBoolean()) c.toLower else c.toUpper)

      val date   = LocalDate.of(1999, 8, 25)
      val data   = Map(
        "value.day"   -> date.getDayOfMonth.toString,
        "value.month" -> toMixedCase(date.getMonth.toString),
        "value.year"  -> date.getYear.toString
      )
      val result = form.bind(data)
      result.value.value mustEqual date
    }

    "must reject non-numeric input" in {
      val data   = Map("value.day" -> "ten", "value.month" -> "Jan", "value.year" -> "twenty")
      val result = form.bind(data)
      result.errors.map(_.message) must contain("registerDateOfBirth.error.invalid")
    }

    "must reject dates with an invalid day (e.g. invalid day 0)" in {
      val data   = Map("value.day" -> "0", "value.month" -> "12", "value.year" -> "2020")
      val result = form.bind(data)
      result.errors must contain only FormError(
        "value",
        "registerDateOfBirth.error.not.real.date",
        Seq("date.error.day")
      )
    }

    "must reject dates with an invalid month (e.g. invalid numeric month 15)" in {
      val data   = Map("value.day" -> "10", "value.month" -> "15", "value.year" -> "2020")
      val result = form.bind(data)
      result.errors must contain only FormError(
        "value",
        "registerDateOfBirth.error.not.real.date",
        Seq("date.error.month")
      )
    }

    "must reject dates with an invalid non-numeric year" in {
      val data   = Map("value.day" -> "10", "value.month" -> "12", "value.year" -> "x")
      val result = form.bind(data)
      result.errors must contain only FormError(
        "value",
        "registerDateOfBirth.error.invalid",
        Seq("date.error.year")
      )
    }

    "must reject dates with an invalid month (e.g. invalid alphabetic month JanX)" in {
      val data   = Map("value.day" -> "10", "value.month" -> "JanX", "value.year" -> "2020")
      val result = form.bind(data)
      result.errors must contain only FormError(
        "value",
        "registerDateOfBirth.error.invalid",
        Seq("date.error.month")
      )
    }

    "must reject dates which are not real dates (e.g. invalid day 31 April)" in {
      val data   = Map("value.day" -> "31", "value.month" -> "4", "value.year" -> "2020")
      val result = form.bind(data)
      result.errors must contain only FormError(
        "value",
        "registerDateOfBirth.error.not.real.date",
        Seq("date.error.day", "date.error.month", "date.error.year")
      )
    }

    "must reject today's date" in {
      val formattedTodaysDate = today.format(displayFormat)
      val data                = Map(
        "value.day"   -> today.getDayOfMonth.toString,
        "value.month" -> today.getMonthValue.toString,
        "value.year"  -> today.getYear.toString
      )
      val result              = form.bind(data)
      result.errors must contain only FormError(
        "value",
        "registerDateOfBirth.error.future.date",
        Seq(formattedTodaysDate, "date.error.day", "date.error.month", "date.error.year")
      )
    }

    "must reject a date in the future" in {
      val future              = maxValidDate.plusDays(2)
      val formattedTodaysDate = today.format(displayFormat)
      val data                = Map(
        "value.day"   -> future.getDayOfMonth.toString,
        "value.month" -> future.getMonthValue.toString,
        "value.year"  -> future.getYear.toString
      )
      val result              = form.bind(data)
      result.errors must contain only FormError(
        "value",
        "registerDateOfBirth.error.future.date",
        Seq(formattedTodaysDate, "date.error.day", "date.error.month", "date.error.year")
      )
    }

    "must reject a date before the minimum (before 1900-01-01)" in {
      val tooOld = minDate.minusDays(1)
      val data   = Map(
        "value.day"   -> tooOld.getDayOfMonth.toString,
        "value.month" -> tooOld.getMonthValue.toString,
        "value.year"  -> tooOld.getYear.toString
      )
      val result = form.bind(data)
      result.errors must contain only FormError(
        "value",
        "registerDateOfBirth.error.past.date",
        Seq("date.error.day", "date.error.month", "date.error.year")
      )
    }

    "must give correct errors when multiple fields missing [day and month]" in {
      val missingDayAndMonth = Map("value.day" -> "", "value.month" -> "", "value.year" -> "2020")
      val result             = form.bind(missingDayAndMonth)
      result.errors must contain only FormError(
        "value",
        "registerDateOfBirth.error.required.day.and.month",
        Seq("date.error.day", "date.error.month")
      )
    }

    "must give correct errors when multiple fields missing [month and year]" in {
      val missingMonthAndYear = Map("value.day" -> "12", "value.month" -> "", "value.year" -> "")
      val result              = form.bind(missingMonthAndYear)
      result.errors must contain only FormError(
        "value",
        "registerDateOfBirth.error.required.month.and.year",
        Seq("date.error.month", "date.error.year")
      )
    }

    "must give correct errors when multiple fields missing [day and year]" in {
      val missingDayAndYear = Map("value.day" -> "", "value.month" -> "10", "value.year" -> "")
      val result            = form.bind(missingDayAndYear)
      result.errors must contain only FormError(
        "value",
        "registerDateOfBirth.error.required.day.and.year",
        Seq("date.error.day", "date.error.year")
      )
    }

    "must unbind LocalDate correctly" in {
      val date = LocalDate.of(1988, 12, 5)
      form.fill(date).data mustEqual Map(
        "value.day"   -> "5",
        "value.month" -> "12",
        "value.year"  -> "1988"
      )
    }
  }
}
