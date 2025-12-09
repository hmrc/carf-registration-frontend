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
import models.OrganisationRegistrationType.*
import models.{IndividualRegistrationType, NormalMode, OrganisationRegistrationType, UserAnswers}
import pages.*
import pages.orgWithoutId.{HaveTradingNamePage, OrgWithoutIdBusinessNamePage, TradingNamePage}
import pages.organisation.{FirstContactEmailPage, FirstContactNamePage, FirstContactPhoneNumberPage, FirstContactPhonePage}
import play.api.mvc.Call
import utils.UserAnswersHelper

import java.time.LocalDate

trait NormalRoutesNavigator extends UserAnswersHelper {

  val normalRoutes: Page => UserAnswers => Call = {

    case IndividualRegistrationTypePage =>
      userAnswers => navigateFromIndividualRegistrationTypePage(userAnswers)

    case OrganisationRegistrationTypePage =>
      _ => routes.RegisteredAddressInUkController.onPageLoad(NormalMode)

    case RegisteredAddressInUkPage =>
      userAnswers => navigateFromRegisteredAddressInUk(userAnswers)

    case HaveUTRPage =>
      userAnswers => navigateFromHaveUTR(userAnswers)

    case YourUniqueTaxpayerReferencePage =>
      userAnswers => navigateFromYourUniqueTaxpayerReference(userAnswers)

    case WhatIsTheNameOfYourBusinessPage =>
      _ => routes.IsThisYourBusinessController.onPageLoad(NormalMode)

    case WhatIsYourNamePage =>
      _ => routes.IsThisYourBusinessController.onPageLoad(NormalMode)

    case IsThisYourBusinessPage =>
      userAnswers => navigateFromIsThisYourBusiness(userAnswers)

    case HaveNiNumberPage =>
      userAnswers => navigateFromHaveNiNumber(userAnswers)

    case NiNumberPage =>
      _ => routes.WhatIsYourNameIndividualController.onPageLoad(NormalMode)

    case WhatIsYourNameIndividualPage =>
      _ => routes.RegisterDateOfBirthController.onPageLoad(NormalMode)

    case OrgWithoutIdBusinessNamePage =>
      _ => controllers.orgWithoutId.routes.HaveTradingNameController.onPageLoad(NormalMode)

    case HaveTradingNamePage =>
      userAnswers => navigateFromHaveTradingName(userAnswers)

    case TradingNamePage =>
      _ =>
        routes.PlaceholderController.onPageLoad(
          "Must redirect to /register/business-without-id/business-address (CARF-162)"
        )

    case RegisterDateOfBirthPage =>
      userAnswers => navigateFromRegisterDateOfBirth(userAnswers)

    case FirstContactNamePage =>
      _ => controllers.organisation.routes.FirstContactEmailController.onPageLoad(NormalMode)

    case FirstContactEmailPage =>
      _ => controllers.organisation.routes.FirstContactPhoneController.onPageLoad(NormalMode)

    case IndividualHavePhonePage =>
      userAnswers => navigateFromIndividualHavePhonePage(userAnswers)

    case FirstContactPhoneNumberPage =>
      _ => routes.OrganisationHaveSecondContactController.onPageLoad(NormalMode)

    case FirstContactPhonePage =>
      userAnswers => navigateFromFirstContactPhonePage(userAnswers)

    case OrganisationHaveSecondContactPage =>
      userAnswers => navigateFromOrganisationHaveSecondContactController(userAnswers)

    case IndividualEmailPage =>
      _ => routes.IndividualHavePhoneController.onPageLoad(NormalMode)

    case OrganisationSecondContactNamePage =>
      _ => routes.OrganisationSecondContactEmailController.onPageLoad(NormalMode)

    case OrganisationSecondContactEmailPage =>
      _ => routes.OrganisationSecondContactHavePhoneController.onPageLoad(NormalMode)

    case OrganisationSecondContactHavePhonePage =>
      userAnswers => navigateFromOrganisationSecondContactHavePhonePage(userAnswers)

    case IndWithoutNinoNamePage =>
      _ =>
        routes.PlaceholderController.onPageLoad(
          "Must redirect to /register/individual-without-id/date-of-birth (CARF-170)"
        )

    case _ =>
      _ => routes.JourneyRecoveryController.onPageLoad()
  }

  private def navigateFromIndividualRegistrationTypePage(userAnswers: UserAnswers): Call =
    userAnswers.get(IndividualRegistrationTypePage) match {
      case Some(IndividualRegistrationType.SoleTrader) =>
        routes.RegisteredAddressInUkController.onPageLoad(NormalMode)
      case Some(IndividualRegistrationType.Individual) =>
        routes.HaveNiNumberController.onPageLoad(NormalMode)
      case _                                           =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromRegisteredAddressInUk(userAnswers: UserAnswers): Call =
    userAnswers.get(RegisteredAddressInUkPage) match {
      case Some(true)  =>
        routes.YourUniqueTaxpayerReferenceController.onPageLoad(NormalMode)
      case Some(false) =>
        routes.HaveUTRController.onPageLoad(NormalMode)
      case None        =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromHaveUTR(userAnswers: UserAnswers): Call =
    userAnswers.get(HaveUTRPage) match {
      case Some(true)  =>
        routes.YourUniqueTaxpayerReferenceController.onPageLoad(NormalMode)
      case Some(false) =>
        if (isSoleTrader(userAnswers)) {
          routes.HaveNiNumberController.onPageLoad(NormalMode)
        } else if (userAnswers.get(OrganisationRegistrationTypePage).isDefined) {
          controllers.orgWithoutId.routes.OrgWithoutIdBusinessNameController.onPageLoad(NormalMode)
        } else {
          routes.JourneyRecoveryController.onPageLoad()
        }
      case None        =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromYourUniqueTaxpayerReference(userAnswers: UserAnswers): Call = {

    val individualRegistrationType: Option[IndividualRegistrationType]     = userAnswers.get(IndividualRegistrationTypePage)
    val organisationRegistrationType: Option[OrganisationRegistrationType] =
      userAnswers.get(OrganisationRegistrationTypePage)

    (individualRegistrationType, organisationRegistrationType) match {
      case (Some(IndividualRegistrationType.SoleTrader), _) | (_, Some(OrganisationRegistrationType.SoleTrader)) =>
        routes.WhatIsYourNameController.onPageLoad(NormalMode)
      case _                                                                                                     =>
        routes.WhatIsTheNameOfYourBusinessController.onPageLoad(NormalMode)
    }
  }

  private def navigateFromIsThisYourBusiness(userAnswers: UserAnswers): Call =
    userAnswers.get(IsThisYourBusinessPage).flatMap(_.pageAnswer) match {
      case Some(true) =>
        if (isSoleTrader(userAnswers)) {
          routes.IndividualEmailController.onPageLoad(NormalMode)
        } else {
          routes.OrgYourContactDetailsController.onPageLoad()
        }

      case Some(false) =>
        if (isCTAutomatched(userAnswers)) {
          routes.ProblemDifferentBusinessController.onPageLoad()
        } else {
          if (isSoleTrader(userAnswers)) {
            routes.PlaceholderController.onPageLoad("Must redirect to /problem/sole-trader-not-identified (CARF-129)")
          } else {
            routes.BusinessNotIdentifiedController.onPageLoad()
          }
        }

      case None =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def isCTAutomatched(userAnswers: UserAnswers): Boolean =
    userAnswers.get(IndexPage).isDefined

  private def navigateFromHaveNiNumber(userAnswers: UserAnswers): Call =
    userAnswers.get(HaveNiNumberPage) match {
      case Some(true)  =>
        routes.NiNumberController.onPageLoad(NormalMode)
      case Some(false) =>
        routes.IndWithoutNinoNameController.onPageLoad(NormalMode)
      case None        => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromHaveTradingName(userAnswers: UserAnswers): Call =
    userAnswers.get(HaveTradingNamePage) match {
      case Some(true) =>
        routes.TradingNameController.onPageLoad(NormalMode)
      case _          =>
        routes.PlaceholderController.onPageLoad(
          "Must redirect to /register/business-without-id/business-address (CARF-162)"
        )
    }

  private def navigateFromRegisterDateOfBirth(userAnswers: UserAnswers): Call =
    userAnswers.get(RegisterDateOfBirthPage) match {
      case Some(_: LocalDate) =>
        routes.RegisterIdentityConfirmedController.onPageLoad()
      case _                  => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromFirstContactPhonePage(userAnswers: UserAnswers): Call =
    userAnswers.get(FirstContactPhonePage) match {
      case Some(true) =>
        controllers.organisation.routes.FirstContactPhoneNumberController.onPageLoad(NormalMode)
      case _          =>
        routes.OrganisationHaveSecondContactController.onPageLoad(NormalMode)
    }

  private def navigateFromOrganisationHaveSecondContactController(userAnswers: UserAnswers): Call =
    userAnswers.get(OrganisationHaveSecondContactPage) match {
      case Some(true)  =>
        routes.OrganisationSecondContactNameController.onPageLoad(NormalMode)
      case Some(false) =>
        routes.CheckYourAnswersController.onPageLoad()
      case None        => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromIndividualHavePhonePage(userAnswers: UserAnswers): Call =
    userAnswers.get(IndividualHavePhonePage) match {
      case Some(true)  =>
        routes.PlaceholderController.onPageLoad(
          "Must redirect to /register/individual-phone (CARF-185)"
        )
      case Some(false) =>
        routes.CheckYourAnswersController.onPageLoad()
      case None        =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromOrganisationSecondContactHavePhonePage(userAnswers: UserAnswers): Call =
    userAnswers.get(OrganisationSecondContactHavePhonePage) match {
      case Some(true)  =>
        routes.PlaceholderController.onPageLoad(
          "Must redirect to /register/second-contact-phone (CARF-252)"
        )
      case Some(false) =>
        routes.CheckYourAnswersController.onPageLoad()
      case None        =>
        routes.JourneyRecoveryController.onPageLoad()
    }
}
