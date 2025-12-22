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
import play.api.data.Form
import play.api.data.validation.{Constraint, Invalid, Valid}

import javax.inject.Inject

class IndividualPhoneNumberFormProvider @Inject() extends Mappings {

  private val phoneNumberRegex     = """^[A-Z0-9 )/(*#+-]+$"""
  private val maxPhoneNumberLength = 24

  private def validPhoneNumberFormat(): Constraint[String] =
    Constraint {
      case str if str.matches(phoneNumberRegex) => Valid
      case _                                    => Invalid("individualPhoneNumber.error.invalid")
    }

  def apply(): Form[String] =
    Form(
      "value" -> text("individualPhoneNumber.error.required")
        .verifying(
          firstError(
            maxLength(maxPhoneNumberLength, "individualPhoneNumber.error.length"),
            validPhoneNumberFormat()
          )
        )
    )
}
