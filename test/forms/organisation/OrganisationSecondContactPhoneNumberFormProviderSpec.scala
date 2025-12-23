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

package forms.organisation

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class OrganisationSecondContactPhoneNumberFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "organisationSecondContactPhoneNumber.error.required"
  val lengthKey   = "organisationSecondContactPhoneNumber.error.length"
  val invalidKey  = "organisationSecondContactPhoneNumber.error.invalid"

  val maxLength = 24

  val form = new OrganisationSecondContactPhoneNumberFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "not bind an invalid phone number" in {
      val result = form.bind(Map(fieldName -> "abcde")).apply(fieldName)

      result.errors must contain(FormError(fieldName, invalidKey))
    }

    "bind valid phone numbers" in {
      val result = form.bind(Map(fieldName -> "07111111111")).apply(fieldName)

      result.errors.isEmpty mustBe true
    }

    "not bind strings longer than the max length" in {
      val longString = "a" * (maxLength + 1)

      val result = form.bind(Map(fieldName -> longString))
      result.errors must contain only FormError(fieldName, lengthKey, Seq.empty)
    }
  }
}
