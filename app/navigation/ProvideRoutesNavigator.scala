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

package navigation

import config.Constants.noneOfTheseValue
import controllers.changeContactDetails.routes as changeDetailsRoutes
import controllers.routes
import models.JourneyType.{IndWithNino, IndWithUtr, IndWithoutId, OrgWithUtr, OrgWithoutId}
import models.RegistrationType.{Individual, SoleTrader}
import models.{NormalMode, ProvideMode, RegistrationType, UserAnswers}
import pages.*
import pages.changeContactDetails.{ChangeDetailsIndividualEmailPage, ChangeDetailsIndividualHavePhonePage, ChangeDetailsIndividualPhoneNumberPage, ChangeDetailsOrgSecondEmailPage, ChangeDetailsOrgSecondNamePage}
import pages.organisation.{HaveUTRPage, NavigatorOnlyIndividualRegistrationTypePage, RegistrationTypePage}
import play.api.Logging
import play.api.mvc.Call
import utils.UserAnswersHelper

import java.time.LocalDate

trait ProvideRoutesNavigator extends UserAnswersHelper with Logging {

  val provideRoutes: Page => UserAnswers => Call = {

    case ChangeDetailsIndividualEmailPage =>
      _ => changeDetailsRoutes.ChangeDetailsIndividualHavePhoneController.onPageLoad(ProvideMode)

    case ChangeDetailsIndividualHavePhonePage =>
      userAnswers => navigateFromProvideHavePhonePage(userAnswers)

    case ChangeDetailsIndividualPhoneNumberPage =>
      _ => changeDetailsRoutes.ChangeIndividualContactDetailsController.onPageLoad()

    case ChangeDetailsOrgSecondEmailPage =>
      _ => changeDetailsRoutes.ChangeOrgSecondContactHavePhoneController.onPageLoad()

    case ChangeDetailsOrgSecondNamePage =>
      _ => changeDetailsRoutes.ChangeOrgSecondContactEmailController.onPageLoad(ProvideMode)

    case _ =>
      _ => routes.JourneyRecoveryController.onPageLoad()
  }

  private def navigateFromProvideHavePhonePage(userAnswers: UserAnswers): Call =
    userAnswers.get(ChangeDetailsIndividualHavePhonePage) match {
      case Some(true)  =>
        changeDetailsRoutes.ChangeIndividualPhoneNumberController.onPageLoad(ProvideMode)
      case Some(false) =>
        changeDetailsRoutes.ChangeIndividualContactDetailsController.onPageLoad()
      case None        =>
        routes.JourneyRecoveryController.onPageLoad()
    }
}
