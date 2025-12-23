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

    "must bind valid UK phone numbers" in {
      val result = form.bind(Map(fieldName -> "07123456789")).apply(fieldName)
      result.value.value mustBe "07123456789"
      result.errors        must be(empty)
    }

    "must bind valid international phone numbers" in {
      val result = form.bind(Map(fieldName -> "+12125550123")).apply(fieldName)
      result.value.value mustBe "+12125550123"
      result.errors        must be(empty)
    }

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    "not bind strings with invalid characters and show the 'invalid' error" in {
      val result = form.bind(Map(fieldName -> "not a phone number")).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey))
    }

    "not bind an unallocated 'not real' number and show the 'invalid' error" in {
      val result = form.bind(Map(fieldName -> "01632 960 001")).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey))
    }

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
