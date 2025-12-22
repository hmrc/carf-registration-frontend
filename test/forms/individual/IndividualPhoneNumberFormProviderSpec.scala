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

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class IndividualPhoneNumberFormProviderSpec extends StringFieldBehaviours {

  val form = new IndividualPhoneNumberFormProvider()()

  val fieldName   = "value"
  val requiredKey = "individualPhoneNumber.error.required"
  val lengthKey   = "individualPhoneNumber.error.length"
  val invalidKey  = "individualPhoneNumber.error.invalid"
  val maxLength   = 24

  ".value" - {

    "must bind valid data" in {
      val result = form.bind(Map(fieldName -> "01234 567890")).apply(fieldName)
      result.value.value mustBe "01234 567890"
    }

    "must bind valid data with allowed special characters" in {
      val result = form.bind(Map(fieldName -> "+44 (808) 157-0192")).apply(fieldName)
      result.value.value mustBe "+44 (808) 157-0192"
    }

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    "not bind strings that exceed max length" in {
      val invalidData = "1" * (maxLength + 1)
      val result      = form.bind(Map(fieldName -> invalidData)).apply(fieldName)
      result.errors must contain(FormError(fieldName, lengthKey, Seq(maxLength)))
    }

    "not bind strings with invalid characters" in {
      val invalidData = "not a phone number!"
      val result      = form.bind(Map(fieldName -> invalidData)).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey))
    }

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
