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

import forms.mappings.Mappings
import models.DateHelper.today
import play.api.data.Form
import play.api.i18n.Messages

import java.time.LocalDate
import javax.inject.Inject

class RegisterDateOfBirthFormProvider @Inject() extends Mappings {

  def apply()(implicit messages: Messages): Form[LocalDate] =
    Form(
      "value" -> localDate(
        invalidKey = "registerDateOfBirth.error.invalid",
        notRealDateKey = "registerDateOfBirth.error.not.real.date",
        allRequiredKey = "registerDateOfBirth.error.required.all",
        dayRequiredKey = "registerDateOfBirth.error.required.day",
        monthRequiredKey = "registerDateOfBirth.error.required.month",
        yearRequiredKey = "registerDateOfBirth.error.required.year",
        dayAndMonthRequiredKey = "registerDateOfBirth.error.required.day.and.month",
        dayAndYearRequiredKey = "registerDateOfBirth.error.required.day.and.year",
        monthAndYearRequiredKey = "registerDateOfBirth.error.required.month.and.year",
        futureDateKey = "registerDateOfBirth.error.future.date",
        pastDateKey = "registerDateOfBirth.error.past.date",
        maxDate = today,
        minDate = LocalDate.of(1901, 1, 1)
      )
    )
}
