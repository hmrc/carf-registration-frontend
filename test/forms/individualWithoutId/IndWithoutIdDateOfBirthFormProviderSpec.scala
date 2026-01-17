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

package forms.individualWithoutId

import forms.behaviours.DateBehaviours
import models.DateHelper.today
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

import java.time.format.DateTimeFormatter
import java.time.LocalDate

class IndWithoutIdDateOfBirthFormProviderSpec extends DateBehaviours {

  private implicit val messages: Messages = stubMessages()
  private val form                        = new IndWithoutIdDateOfBirthFormProvider()()

  private val minDate       = LocalDate.of(1901, 1, 1)
  private val maxValidDate  = today.minusDays(1)
  private val displayFormat = DateTimeFormatter.ofPattern("d MMMM yyyy")

  ".value" - {
    val validData = datesBetween(min = minDate, max = maxValidDate)
    behave like dateField(form, "value", validData)

    "fail to bind an empty date" in {
      val data   = Map("value.day" -> "", "value.month" -> "", "value.year" -> "")
      val result = form.bind(data)
      result.errors must contain allOf (
        FormError("value.day", "indWithoutIdDateOfBirth.error.required.all"),
        FormError("value.month", "indWithoutIdDateOfBirth.error.required.all"),
        FormError("value.year", "indWithoutIdDateOfBirth.error.required.all")
      )
    }

    "must fail with 'not real date' error for an invalid date like 30th Feb" in {
      val data   = Map(
        "value.day"   -> "30",
        "value.month" -> "2",
        "value.year"  -> "2024"
      )
      val result = form.bind(data)
      result.errors must contain allOf (
        FormError("value.day", "indWithoutIdDateOfBirth.error.not.real.date"),
        FormError("value.month", "indWithoutIdDateOfBirth.error.not.real.date"),
        FormError("value.year", "indWithoutIdDateOfBirth.error.not.real.date")
      )
    }

    "must fail with 'day and month required' error when only year is provided" in {
      val data   = Map("value.year" -> "2000")
      val result = form.bind(data)
      result.errors must contain allOf (
        FormError("value.day", "indWithoutIdDateOfBirth.error.required.day.and.month"),
        FormError("value.month", "indWithoutIdDateOfBirth.error.required.day.and.month")
      )
    }

    "must fail with 'day and year required' error when only month is provided" in {
      val data   = Map("value.month" -> "1")
      val result = form.bind(data)
      result.errors must contain allOf (
        FormError("value.day", "indWithoutIdDateOfBirth.error.required.day.and.year"),
        FormError("value.year", "indWithoutIdDateOfBirth.error.required.day.and.year")
      )
    }

    "must fail with 'month and year required' error when only day is provided" in {
      val data   = Map("value.day" -> "1")
      val result = form.bind(data)
      result.errors must contain allOf (
        FormError("value.month", "indWithoutIdDateOfBirth.error.required.month.and.year"),
        FormError("value.year", "indWithoutIdDateOfBirth.error.required.month.and.year")
      )
    }

    "must fail with 'day required' error when month and year are provided" in {
      val data   = Map(
        "value.month" -> "1",
        "value.year"  -> "2000"
      )
      val result = form.bind(data)
      result.errors must contain only FormError("value.day", "indWithoutIdDateOfBirth.error.required.day")
    }

    "must fail with 'month required' error when day and year are provided" in {
      val data   = Map(
        "value.day"  -> "1",
        "value.year" -> "2000"
      )
      val result = form.bind(data)
      result.errors must contain only FormError("value.month", "indWithoutIdDateOfBirth.error.required.month")
    }

    "must fail with 'year required' error when day and month are provided" in {
      val data   = Map(
        "value.day"   -> "1",
        "value.month" -> "1"
      )
      val result = form.bind(data)
      result.errors must contain only FormError("value.year", "indWithoutIdDateOfBirth.error.required.year")
    }

    "must reject today's date" in {
      val data                = Map(
        "value.day"   -> today.getDayOfMonth.toString,
        "value.month" -> today.getMonthValue.toString,
        "value.year"  -> today.getYear.toString
      )
      val result              = form.bind(data)
      val formattedTodaysDate = today.format(displayFormat)
      result.errors must contain allOf (
        FormError("value.day", "indWithoutIdDateOfBirth.error.future.date", Seq(formattedTodaysDate)),
        FormError("value.month", "indWithoutIdDateOfBirth.error.future.date", Seq(formattedTodaysDate)),
        FormError("value.year", "indWithoutIdDateOfBirth.error.future.date", Seq(formattedTodaysDate))
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
      result.errors must contain allOf (
        FormError("value.day", "indWithoutIdDateOfBirth.error.past.date"),
        FormError("value.month", "indWithoutIdDateOfBirth.error.past.date"),
        FormError("value.year", "indWithoutIdDateOfBirth.error.past.date")
      )
    }
  }
}
