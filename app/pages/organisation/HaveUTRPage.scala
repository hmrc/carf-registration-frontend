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

package pages.organisation

import config.Constants.*
import models.UserAnswers
import pages.QuestionPage
import pages.individual.NiNumberPage
import play.api.libs.json.JsPath

import scala.util.{Success, Try}

case object HaveUTRPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "haveUTR"

  private val withoutUtrPages =
    indGeneralPage ++ orgWithoutIdPages ++ indWithNinoPages ++ indWithoutIdPages

  override def cleanup(
      newValue: Boolean,
      updatedUserAnswers: UserAnswers,
      hasChanged: Boolean
  ): Try[UserAnswers] =
    if (hasChanged) {
      if (newValue) {
        updatedUserAnswers.clearMatchFlagAndSafeId.remove(withoutUtrPages)
      } else {
        // CARF-545: If NINO was previously provided, don't clear match flag and safeId here
        // so that we can redirect to CYA from /change-have-ni-number if it was previously matched
        val userAnswersWithMatchFlagMaybeCleared =
          if (updatedUserAnswers.get(NiNumberPage).isDefined) updatedUserAnswers
          else updatedUserAnswers.clearMatchFlagAndSafeId
        userAnswersWithMatchFlagMaybeCleared.remove(withUtrPages)
      }
    } else {
      Success(updatedUserAnswers)
    }
}
