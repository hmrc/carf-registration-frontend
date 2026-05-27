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

import controllers.changeContactDetails.routes as changeDetailsRoutes
import controllers.routes
import models.{ProvideMode, UserAnswers}
import pages.*
import pages.changeContactDetails.*
import play.api.Logging
import play.api.mvc.Call
import utils.UserAnswersHelper

trait ProvideRoutesNavigator extends UserAnswersHelper with Logging {

  val provideRoutes: Page => UserAnswers => Call = {

    case ChangeDetailsIndividualEmailPage =>
      _ => changeDetailsRoutes.ChangeDetailsIndividualHavePhoneController.onPageLoad(ProvideMode)

    case ChangeDetailsIndividualHavePhonePage =>
      userAnswers => navigateFromProvideHavePhonePage(userAnswers)

    case ChangeDetailsIndividualPhoneNumberPage =>
      _ => changeDetailsRoutes.ChangeIndividualContactDetailsController.onPageLoad()

    case ChangeDetailsOrgFirstNamePage =>
      _ => changeDetailsRoutes.ChangeOrgFirstContactEmailController.onPageLoad(ProvideMode)

    case ChangeDetailsOrgFirstEmailPage =>
      _ => changeDetailsRoutes.ChangeOrgFirstContactHavePhoneController.onPageLoad(ProvideMode)

    case ChangeDetailsOrgFirstHavePhonePage =>
      userAnswers => navigateFromProvideOrgFirstHavePhonePage(userAnswers)

    case ChangeDetailsOrgFirstPhoneNumberPage =>
      _ => changeDetailsRoutes.ChangeOrgHaveSecondContactController.onPageLoad(ProvideMode)

    case ChangeDetailsOrgHaveSecondContactPage =>
      userAnswers => navigateFromProvideOrgHaveSecondContactPage(userAnswers)

    case ChangeDetailsOrgSecondNamePage =>
      _ => changeDetailsRoutes.ChangeOrgSecondContactEmailController.onPageLoad(ProvideMode)

    case ChangeDetailsOrgSecondEmailPage =>
      _ => changeDetailsRoutes.ChangeOrgSecondContactHavePhoneController.onPageLoad(ProvideMode)

    case ChangeDetailsOrgSecondHavePhonePage =>
      userAnswers => navigateFromProvideOrgSecondHavePhonePage(userAnswers)

    case ChangeDetailsOrgSecondPhoneNumberPage =>
      _ => changeDetailsRoutes.ChangeOrganisationContactDetailsController.onPageLoad()

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

  private def navigateFromProvideOrgFirstHavePhonePage(userAnswers: UserAnswers): Call =
    userAnswers.get(ChangeDetailsOrgFirstHavePhonePage) match {
      case Some(true)  => changeDetailsRoutes.ChangeOrgFirstContactPhoneNumberController.onPageLoad(ProvideMode)
      case Some(false) => changeDetailsRoutes.ChangeOrgHaveSecondContactController.onPageLoad(ProvideMode)
      case None        => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromProvideOrgHaveSecondContactPage(userAnswers: UserAnswers): Call =
    userAnswers.get(ChangeDetailsOrgHaveSecondContactPage) match {
      case Some(true)  => changeDetailsRoutes.ChangeOrgSecondContactNameController.onPageLoad(ProvideMode)
      case Some(false) => changeDetailsRoutes.ChangeOrganisationContactDetailsController.onPageLoad()
      case None        => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromProvideOrgSecondHavePhonePage(userAnswers: UserAnswers): Call =
    userAnswers.get(ChangeDetailsOrgSecondHavePhonePage) match {
      case Some(true)  => changeDetailsRoutes.ChangeOrgSecondContactPhoneNumberController.onPageLoad(ProvideMode)
      case Some(false) => changeDetailsRoutes.ChangeOrganisationContactDetailsController.onPageLoad()
      case None        => routes.JourneyRecoveryController.onPageLoad()
    }
}
