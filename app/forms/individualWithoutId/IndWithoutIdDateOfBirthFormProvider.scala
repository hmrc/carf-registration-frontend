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

import forms.mappings.Mappings
import models.DateHelper.today
import play.api.data.Form
import play.api.i18n.Messages

import java.time.LocalDate
import javax.inject.Inject

class IndWithoutIdDateOfBirthFormProvider @Inject() extends Mappings {

  def apply()(implicit messages: Messages): Form[LocalDate] =
    Form(
      "value" -> localDate(
        invalidKey = "indWithoutIdDateOfBirth.error.invalid",
        notRealDateKey = "indWithoutIdDateOfBirth.error.not.real.date",
        allRequiredKey = "indWithoutIdDateOfBirth.error.required.all",
        dayRequiredKey = "indWithoutIdDateOfBirth.error.required.day",
        monthRequiredKey = "indWithoutIdDateOfBirth.error.required.month",
        yearRequiredKey = "indWithoutIdDateOfBirth.error.required.year",
        dayAndMonthRequiredKey = "indWithoutIdDateOfBirth.error.required.day.and.month",
        dayAndYearRequiredKey = "indWithoutIdDateOfBirth.error.required.day.and.year",
        monthAndYearRequiredKey = "indWithoutIdDateOfBirth.error.required.month.and.year",
        futureDateKey = "indWithoutIdDateOfBirth.error.future.date",
        pastDateKey = "indWithoutIdDateOfBirth.error.past.date",
        maxDate = today.minusDays(1),
        minDate = LocalDate.of(1901, 1, 1)
      )
    )
}
