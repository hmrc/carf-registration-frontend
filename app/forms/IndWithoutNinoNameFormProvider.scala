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

import config.Constants
import config.Constants.individualNameRegex
import forms.mappings.Mappings
import models.Name
import play.api.data.Form
import play.api.data.Forms.*

import javax.inject.Inject
class IndWithoutNinoNameFormProvider @Inject() extends Mappings {

  private val maxLength = 35

  def apply(): Form[Name] = Form(
    mapping(
      "givenName"  -> validatedText(
        "indWithoutNinoName.error.givenName.required",
        "indWithoutNinoName.error.givenName.invalid",
        "indWithoutNinoName.error.givenName.length",
        individualNameRegex,
        maxLength
      ),
      "familyName" -> validatedText(
        "indWithoutNinoName.error.familyName.required",
        "indWithoutNinoName.error.familyName.invalid",
        "indWithoutNinoName.error.familyName.length",
        individualNameRegex,
        maxLength
      )
    )(Name.apply)(name => Some((name.firstName, name.lastName)))
  )

}
