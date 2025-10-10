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

package forms

import config.FrontendAppConfig
import forms.behaviours.StringFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError
import javax.inject.Inject

class BusinessWithoutIdBusinessNameFormProviderSpec @Inject() (config: FrontendAppConfig)
    extends StringFieldBehaviours {
  val form                  = new BusinessWithoutIdBusinessNameFormProvider(config)("")
  val requiredErrorKey      = "businessWithoutIdBusinessName.error.required"
  val lengthErrorKey        = "businessWithoutIdBusinessName.error.maximumLength"
  val invalidFormatErrorKey = "businessWithoutIdBusinessName.error.invalidFormat"
  val maxLength             = 105

  ".value" - {
    val fieldName = "value"
    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator = config.validBusinessNameChars
    )
    behave like fieldWithValidCharsLongerThanMaxLength(
      form,
      fieldName,
      config.validBusinessNameChars,
      maxLength = maxLength,
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
      "A@$%^&a![]{}*",
      FormError(fieldName, invalidFormatErrorKey)
    )
  }
}
