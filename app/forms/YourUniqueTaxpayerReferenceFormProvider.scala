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

import javax.inject.Inject
import forms.mappings.Mappings
import play.api.data.Form
import models.UniqueTaxpayerReference
import play.api.data.Forms.mapping
import scala.util.matching.Regex
import javax.inject.Inject

class YourUniqueTaxpayerReferenceFormProvider @Inject() extends Mappings {

//  val utrRegex: Regex = "[0-9]{13}".r
  private val utrRegex = "^[0-9]*$"

  def apply(taxType: UniqueTaxpayerReference) =
  Form(
    mapping("value" -> validatedUTR("yourUniqueTaxpayerReference.error.required",
      "yourUniqueTaxpayerReference.error.invalid",
      "yourUniqueTaxpayerReference.error.invalidFormat", utrRegex, taxType))(
      UniqueTaxpayerReference.apply
    )(
      UniqueTaxpayerReference.unapply
    )
  )
}










//Form(
//  "value" -> text("yourUniqueTaxpayerReference.error.required")
//    .verifying(maxLength(13, "yourUniqueTaxpayerReference.error.length"))
//)