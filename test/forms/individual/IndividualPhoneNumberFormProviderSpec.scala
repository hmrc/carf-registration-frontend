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

    "must bind a valid UK phone number" in {
      val result = form.bind(Map(fieldName -> "07123 456789"))
      result.errors must be(empty)
      result.get  mustBe "07123 456789"
    }

    "must bind a valid international phone number" in {
      val result = form.bind(Map(fieldName -> "+33 6 00 00 00 00"))
      result.errors must be(empty)
      result.get  mustBe "+33 6 00 00 00 00"
    }

    "must bind a valid number with parentheses" in {
      val result = form.bind(Map(fieldName -> "(0121) 234 5678"))
      result.errors must be(empty)
      result.get  mustBe "(0121) 234 5678"
    }

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "not bind strings with invalid characters" in {
      val result = form.bind(Map(fieldName -> "not a phone number!")).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey))
    }

    "not bind strings longer than the max length" in {
      val longString = "1" * (maxLength + 1)
      val result     = form.bind(Map(fieldName -> longString))
      result.errors must contain only FormError(fieldName, lengthKey, Seq.empty)
    }
  }
}
