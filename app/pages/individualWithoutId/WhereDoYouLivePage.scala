/*
 * Copyright 2026 HM Revenue & Customs
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

package pages.individualWithoutId

import models.UserAnswers
import pages.QuestionPage
import play.api.libs.json.JsPath
import queries.Settable

import scala.util.{Success, Try}

case object WhereDoYouLivePage extends QuestionPage[Boolean] {

  private def indNoIdUkPages: List[Settable[_]] = List(
    IndFindAddressAdditionalCallUa,
    IndFindAddressPage,
    AddressLookupPage,
    AddressUPRNUserAnswers,
    IndWithoutIdAddressPagePrePop,
    IndWithoutIdChooseAddressPage,
    IndWithoutIdSelectedChooseAddressPage,
    IndWithoutIdUkAddressInUserAnswers
  )

  private def indNoIdNonUkPages: List[Settable[_]] = List(
    IndWithoutIdAddressNonUkPage
  )

  override def path: JsPath = JsPath \ toString

  override def toString: String = "whereDoYouLive"

  override def cleanup(newValue: Boolean, updatedUserAnswers: UserAnswers, hasChanged: Boolean): Try[UserAnswers] =
    if (hasChanged) {
      if newValue then updatedUserAnswers.remove(indNoIdNonUkPages) else updatedUserAnswers.remove(indNoIdUkPages)
    } else {
      Success(updatedUserAnswers)
    }
}
