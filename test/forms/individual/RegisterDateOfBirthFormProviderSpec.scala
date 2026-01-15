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

package forms.individual

import forms.behaviours.DateBehaviours
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneOffset}

class RegisterDateOfBirthFormProviderSpec extends DateBehaviours {
  private implicit val messages: Messages = stubMessages()
  private val form                        = new RegisterDateOfBirthFormProvider()()
  private val minDate                     = LocalDate.of(1901, 1, 1)
  private val todayDate                   = LocalDate.now(ZoneOffset.UTC)
  private val maxValidDate                = todayDate.minusDays(1)
  val displayFormat                       = DateTimeFormatter.ofPattern("d MMMM yyyy")

  ".value" - {
    val validData = datesBetween(min = minDate, max = maxValidDate)
    behave like dateField(form, "value", validData)

    "fail to bind an empty date" in {
      val data   = Map("value.day" -> "", "value.month" -> "", "value.year" -> "")
      val result = form.bind(data)
      result.errors must contain theSameElementsAs Seq(
        FormError("value.day", "registerDateOfBirth.error.required.all"),
        FormError("value.month", "registerDateOfBirth.error.required.all"),
        FormError("value.year", "registerDateOfBirth.error.required.all")
      )
    }

    "must bind the minimum allowed date (1901-01-01)" in {
      val data   = Map(
        "value.day"   -> "1",
        "value.month" -> "1",
        "value.year"  -> "1901"
      )
      val result = form.bind(data)
      result.value.value mustEqual LocalDate.of(1901, 1, 1)
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
        s.zipWithIndex.map { case (c, i) => if (i % 2 == 0) c.toLower else c.toUpper }.mkString
      val date                           = LocalDate.of(1999, 8, 25)
      val data                           = Map(
        "value.day"   -> date.getDayOfMonth.toString,
        "value.month" -> toMixedCase("August"),
        "value.year"  -> date.getYear.toString
      )
      val result                         = form.bind(data)
      result.value.value mustEqual date
    }

    "must reject non-numeric input" in {
      val data   = Map("value.day" -> "ten", "value.month" -> "Jan", "value.year" -> "twenty")
      val result = form.bind(data)
      result.errors must contain theSameElementsAs Seq(
        FormError("value.day", "registerDateOfBirth.error.invalid"),
        FormError("value.year", "registerDateOfBirth.error.invalid")
      )
    }

    "must reject dates with an invalid day (e.g. invalid day 0)" in {
      val data   = Map("value.day" -> "0", "value.month" -> "12", "value.year" -> "2020")
      val result = form.bind(data)
      result.errors must contain only FormError(
        "value.day",
        "registerDateOfBirth.error.not.real.date"
      )
    }

    "must reject dates with an invalid month (e.g. invalid numeric month 15)" in {
      val data   = Map("value.day" -> "10", "value.month" -> "15", "value.year" -> "2020")
      val result = form.bind(data)
      result.errors must contain only FormError(
        "value.month",
        "registerDateOfBirth.error.not.real.date"
      )
    }

    "must reject dates with an invalid non-numeric year" in {
      val data   = Map("value.day" -> "10", "value.month" -> "12", "value.year" -> "x")
      val result = form.bind(data)
      result.errors must contain only FormError(
        "value.year",
        "registerDateOfBirth.error.invalid"
      )
    }

    "must reject dates with an invalid month (e.g. invalid alphabetic month JanX)" in {
      val data   = Map("value.day" -> "10", "value.month" -> "JanX", "value.year" -> "2020")
      val result = form.bind(data)
      result.errors must contain only FormError(
        "value.month",
        "registerDateOfBirth.error.invalid"
      )
    }

    "must reject dates with an invalid month (janf, janu, januaryy, etc)" in {
      val invalidMonths = Seq("janf", "janu", "januaryy", "augu", "augustt", "au")
      invalidMonths.foreach { m =>
        val data   = Map("value.day" -> "10", "value.month" -> m, "value.year" -> "2020")
        val result = form.bind(data)
        result.errors must contain only FormError(
          "value.month",
          "registerDateOfBirth.error.invalid"
        )
      }
    }

    "must reject dates which are not real dates (e.g. invalid day 31 April)" in {
      val data   = Map("value.day" -> "31", "value.month" -> "4", "value.year" -> "2020")
      val result = form.bind(data)
      result.errors must contain theSameElementsAs Seq(
        FormError("value.day", "registerDateOfBirth.error.not.real.date"),
        FormError("value.month", "registerDateOfBirth.error.not.real.date"),
        FormError("value.year", "registerDateOfBirth.error.not.real.date")
      )
    }

    "must reject today's date" in {
      val data                = Map(
        "value.day"   -> todayDate.getDayOfMonth.toString,
        "value.month" -> todayDate.getMonthValue.toString,
        "value.year"  -> todayDate.getYear.toString
      )
      val result              = form.bind(data)
      val formattedTodaysDate = todayDate.format(displayFormat)
      result.errors must contain theSameElementsAs Seq(
        FormError("value.day", "registerDateOfBirth.error.future.date", Seq(formattedTodaysDate)),
        FormError("value.month", "registerDateOfBirth.error.future.date", Seq(formattedTodaysDate)),
        FormError("value.year", "registerDateOfBirth.error.future.date", Seq(formattedTodaysDate))
      )
    }

    "must reject a date in the future" in {
      val future              = todayDate.plusDays(1)
      val data                = Map(
        "value.day"   -> future.getDayOfMonth.toString,
        "value.month" -> future.getMonthValue.toString,
        "value.year"  -> future.getYear.toString
      )
      val result              = form.bind(data)
      val formattedFutureDate = todayDate.format(displayFormat)
      result.errors must contain theSameElementsAs Seq(
        FormError("value.day", "registerDateOfBirth.error.future.date", Seq(formattedFutureDate)),
        FormError("value.month", "registerDateOfBirth.error.future.date", Seq(formattedFutureDate)),
        FormError("value.year", "registerDateOfBirth.error.future.date", Seq(formattedFutureDate))
      )
    }

    "must reject a date before the minimum (before 1901-01-01)" in {
      val tooOld = minDate.minusDays(1)
      val data   = Map(
        "value.day"   -> tooOld.getDayOfMonth.toString,
        "value.month" -> tooOld.getMonthValue.toString,
        "value.year"  -> tooOld.getYear.toString
      )
      val result = form.bind(data)
      result.errors must contain theSameElementsAs Seq(
        FormError("value.day", "registerDateOfBirth.error.past.date"),
        FormError("value.month", "registerDateOfBirth.error.past.date"),
        FormError("value.year", "registerDateOfBirth.error.past.date")
      )
    }

    "must give correct errors when multiple fields missing [day and month]" in {
      val missingDayAndMonth = Map("value.day" -> "", "value.month" -> "", "value.year" -> "2020")
      val result             = form.bind(missingDayAndMonth)
      result.errors must contain theSameElementsAs Seq(
        FormError("value.day", "registerDateOfBirth.error.required.day.and.month"),
        FormError("value.month", "registerDateOfBirth.error.required.day.and.month")
      )
    }

    "must give correct errors when multiple fields missing [month and year]" in {
      val missingMonthAndYear = Map("value.day" -> "12", "value.month" -> "", "value.year" -> "")
      val result              = form.bind(missingMonthAndYear)
      result.errors must contain theSameElementsAs Seq(
        FormError("value.month", "registerDateOfBirth.error.required.month.and.year"),
        FormError("value.year", "registerDateOfBirth.error.required.month.and.year")
      )
    }

    "must give correct errors when multiple fields missing [day and year]" in {
      val missingDayAndYear = Map("value.day" -> "", "value.month" -> "10", "value.year" -> "")
      val result            = form.bind(missingDayAndYear)
      result.errors must contain theSameElementsAs Seq(
        FormError("value.day", "registerDateOfBirth.error.required.day.and.year"),
        FormError("value.year", "registerDateOfBirth.error.required.day.and.year")
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
