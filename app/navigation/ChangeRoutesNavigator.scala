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

import controllers.routes
import controllers.routes.CheckYourAnswersController
import models.RegistrationType.SoleTrader
import models.{ChangeMode, NormalMode, RegistrationType, UserAnswers}
import pages.individual.{HaveNiNumberPage, IndividualEmailPage, NiNumberPage, WhatIsYourNameIndividualPage}
import pages.orgWithoutId.{HaveTradingNamePage, OrgWithoutIdBusinessNamePage, OrganisationBusinessAddressPage, TradingNamePage}
import pages.organisation.*
import pages.*
import play.api.libs.json.Reads
import play.api.mvc.Call
import utils.UserAnswersHelper

trait ChangeRoutesNavigator extends UserAnswersHelper {

  val checkRouteMap: Page => UserAnswers => Call = {
    case NavigatorOnlyIndividualRegistrationTypePage =>
      userAnswers => navigateFromIndividualRegistrationTypePage(userAnswers)

    case NavigatorOnlyOrganisationRegistrationTypePage =>
      _ => controllers.routes.RegisteredAddressInUkController.onPageLoad(ChangeMode)

    case RegisteredAddressInUkPage =>
      userAnswers => navigateFromRegisteredAddressInUk(userAnswers)

    case HaveUTRPage =>
      userAnswers => navigateFromHaveUTR(userAnswers)

    case YourUtrPageForNavigatorOnly =>
      userAnswers => navigateFromYourUniqueTaxpayerReference(userAnswers)

    case WhatIsTheNameOfYourBusinessPage =>
      _ => routes.IsThisYourBusinessController.onPageLoad(ChangeMode)

    case WhatIsYourNamePage =>
      _ => routes.IsThisYourBusinessController.onPageLoad(ChangeMode)

    case IsThisYourBusinessPage => userAnswers => navigateFromIsThisYourBusiness(userAnswers)

    case OrgWithoutIdBusinessNamePage =>
      _ => CheckYourAnswersController.onPageLoad()

    case HaveTradingNamePage =>
      userAnswers => navigateFromHaveTradingName(userAnswers)

    case TradingNamePage =>
      _ => CheckYourAnswersController.onPageLoad()

    case OrganisationBusinessAddressPage =>
      userAnswers => navigateFromOrganisationBusinessAddress(userAnswers)

    case HaveNiNumberPage =>
      userAnswers => navigateFromHaveNiNumber(userAnswers)

    case _ => _ => routes.JourneyRecoveryController.onPageLoad()
  }

  private def navigateFromHaveTradingName(userAnswers: UserAnswers): Call =
    userAnswers.get(HaveTradingNamePage) match {
      case Some(false) =>
        CheckYourAnswersController.onPageLoad()
      case Some(true)  =>
        checkNextPageForValueThenRoute(
          userAnswers = userAnswers,
          page = TradingNamePage,
          callWhenNotAnswered = controllers.orgWithoutId.routes.TradingNameController.onPageLoad(ChangeMode)
        )
      case None        =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromOrganisationBusinessAddress(userAnswers: UserAnswers): Call =
    userAnswers.get(OrganisationBusinessAddressPage) match {
      case Some(_) =>
        if (userAnswers.get(FirstContactNamePage).isDefined) {
          CheckYourAnswersController.onPageLoad()
        } else {
          controllers.organisation.routes.FirstContactNameController.onPageLoad(ChangeMode)
        }
      case None    =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromHaveNiNumber(userAnswers: UserAnswers): Call =
    userAnswers.get(HaveNiNumberPage) match {
      case Some(true) =>
        checkNextPageForValueThenRoute(
          userAnswers = userAnswers,
          page = NiNumberPage,
          callWhenNotAnswered = controllers.individual.routes.NiNumberController.onPageLoad(NormalMode)
        )

      case Some(false) =>
        checkNextPageForValueThenRoute(
          userAnswers = userAnswers,
          page = WhatIsYourNameIndividualPage,
          callWhenNotAnswered =
            controllers.individualWithoutId.routes.IndWithoutNinoNameController.onPageLoad(NormalMode)
        )

      case None =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def checkNextPageForValueThenRoute[A](
      userAnswers: UserAnswers,
      page: QuestionPage[A],
      callWhenNotAnswered: Call,
      callWhenAlreadyAnswered: Call = CheckYourAnswersController.onPageLoad()
  )(implicit rds: Reads[A]): Call = {
    val answerExists = userAnswers.get(page).fold(false)(_ => true)
    if (answerExists) {
      callWhenAlreadyAnswered
    } else {
      callWhenNotAnswered
    }
  }

  private def navigateFromIndividualRegistrationTypePage(userAnswers: UserAnswers): Call =
    userAnswers.get(RegistrationTypePage) match {
      case Some(SoleTrader)                  =>
        controllers.routes.RegisteredAddressInUkController.onPageLoad(ChangeMode)
      case Some(RegistrationType.Individual) =>
        controllers.individual.routes.HaveNiNumberController.onPageLoad(ChangeMode)
      case _                                 =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromRegisteredAddressInUk(userAnswers: UserAnswers): Call =
    userAnswers.get(RegisteredAddressInUkPage) match {
      case Some(true)  =>
        controllers.organisation.routes.YourUniqueTaxpayerReferenceController.onPageLoad(ChangeMode)
      case Some(false) =>
        controllers.organisation.routes.HaveUTRController.onPageLoad(ChangeMode)
      case None        =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromHaveUTR(userAnswers: UserAnswers): Call =
    userAnswers.get(HaveUTRPage) match {
      case Some(true)  =>
        controllers.organisation.routes.YourUniqueTaxpayerReferenceController.onPageLoad(ChangeMode)
      case Some(false) =>
        if (isSoleTrader(userAnswers)) {
          controllers.individual.routes.HaveNiNumberController.onPageLoad(ChangeMode)
        } else {
          checkNextPageForValueThenRoute(
            userAnswers = userAnswers,
            page = OrgWithoutIdBusinessNamePage,
            callWhenNotAnswered =
              controllers.orgWithoutId.routes.OrgWithoutIdBusinessNameController.onPageLoad(NormalMode)
          )
        }
      case None        =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromYourUniqueTaxpayerReference(userAnswers: UserAnswers): Call =
    if (isSoleTrader(userAnswers)) {
      controllers.organisation.routes.WhatIsYourNameController.onPageLoad(ChangeMode)
    } else {
      controllers.organisation.routes.WhatIsTheNameOfYourBusinessController.onPageLoad(ChangeMode)
    }

  private def navigateFromIsThisYourBusiness(userAnswers: UserAnswers): Call =
    userAnswers.get(IsThisYourBusinessPage).flatMap(_.pageAnswer) match {
      case Some(true) =>
        if (isSoleTrader(userAnswers)) {
          checkNextPageForValueThenRoute(
            userAnswers = userAnswers,
            page = IndividualEmailPage,
            callWhenNotAnswered = controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode)
          )
        } else {
          checkNextPageForValueThenRoute(
            userAnswers = userAnswers,
            page = FirstContactNamePage,
            callWhenNotAnswered = controllers.organisation.routes.OrgYourContactDetailsController.onPageLoad()
          )
        }

      case Some(false) =>
        if (userAnswers.isCtAutoMatched) {
          controllers.organisation.routes.ProblemDifferentBusinessController.onPageLoad()
        } else {
          if (isSoleTrader(userAnswers)) {
            controllers.individual.routes.ProblemSoleTraderNotIdentifiedController.onPageLoad()
          } else {
            controllers.organisation.routes.BusinessNotIdentifiedController.onPageLoad()
          }
        }

      case None =>
        routes.JourneyRecoveryController.onPageLoad()
    }

}
