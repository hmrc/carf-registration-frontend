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

package forms.orgWithoutId

import base.TestConstants.{businessNameWithInvalidChars, validBusinessNameChars}
import config.Constants.validBusinessNameMaxLength
import forms.behaviours.StringFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError

class OrgWithoutIdBusinessNameFormProviderSpec extends StringFieldBehaviours {
  val form                  = new OrgWithoutIdBusinessNameFormProvider()()
  val requiredErrorKey      = "businessName.error.required"
  val lengthErrorKey        = "businessName.error.maximumLength"
  val invalidFormatErrorKey = "businessName.error.invalidFormat"

  ".value" - {
    val fieldName = "value"
    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator = validBusinessNameChars
    )
    behave like fieldWithValidCharsLongerThanMaxLength(
      form,
      fieldName,
      validBusinessNameChars,
      maxLength = validBusinessNameMaxLength,
      lengthError = FormError(fieldName, lengthErrorKey)
    )
    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredErrorKey)
    )
    behave like fieldWithInvalidData(
      form,
      fieldName,
      businessNameWithInvalidChars,
      FormError(fieldName, invalidFormatErrorKey)
    )
  }
}
