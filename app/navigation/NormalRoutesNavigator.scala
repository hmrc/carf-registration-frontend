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
      _ => routes.FirstContactEmailController.onPageLoad(NormalMode)

    case FirstContactEmailPage =>
      _ => routes.FirstContactPhoneController.onPageLoad(NormalMode)

    case IndividualHavePhonePage =>
      userAnswers => navigateFromIndividualHavePhonePage(userAnswers)

    case FirstContactPhoneNumberPage =>
      _ => routes.PlaceholderController.onPageLoad("Must redirect to /register/have-second-contact (CARF-182)")

    case FirstContactPhonePage =>
      userAnswers => navigateFromFirstContactPhonePage(userAnswers)

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
          routes.PlaceholderController.onPageLoad("Must redirect to /register/individual-email (CARF-183)")
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
            routes.PlaceholderController.onPageLoad("Must redirect to /problem/business-not-identified (CARF-147)")
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
        routes.PlaceholderController.onPageLoad("Must redirect to /individual-without-id/name (CARF-169)")
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
        routes.PlaceholderController.onPageLoad(
          "Must redirect to /register/identity-confirmed (CARF-168)"
        )
      case _                  => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromFirstContactPhonePage(userAnswers: UserAnswers): Call =
    userAnswers.get(FirstContactPhonePage) match {
      case Some(true) =>
        routes.FirstContactPhoneNumberController.onPageLoad(NormalMode)
      case _          =>
        routes.PlaceholderController.onPageLoad(
          "Must redirect to /register/have-second-contact (CARF-182)"
        )
    }

  private def navigateFromIndividualHavePhonePage(userAnswers: UserAnswers): Call =
    userAnswers.get(IndividualHavePhonePage) match {
      case Some(true)  =>
        routes.IndividualHavePhoneController.onPageLoad(NormalMode)
      case Some(false) =>
        findFirstMissingPageForIndividualOrSoleTrader(userAnswers)
      case None        =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def findFirstMissingPageForIndividualOrSoleTrader(userAnswers: UserAnswers): Call = {

    val missingPageChecks: List[Option[Call]] =
      if (isSoleTrader(userAnswers)) {
        List(
          if (userAnswers.get(IndividualRegistrationTypePage).isEmpty) {
            Some(routes.IndividualRegistrationTypeController.onPageLoad(NormalMode))
          } else {
            None
          },
          if (userAnswers.get(RegisteredAddressInUkPage).isEmpty) {
            Some(routes.RegisteredAddressInUkController.onPageLoad(NormalMode))
          } else {
            None
          },
          if (userAnswers.get(IsThisYourBusinessPage).isEmpty) {
            Some(routes.IsThisYourBusinessController.onPageLoad(NormalMode))
          } else {
            None
          }
          // CARF-183 if (userAnswers.get(IndividualContactEmailPage).isEmpty) Some(routes.IndividualContactEmailController.onPageLoad(NormalMode)) else None
        )
      } else {
        List(
          if (userAnswers.get(IndividualRegistrationTypePage).isEmpty) {
            Some(routes.IndividualRegistrationTypeController.onPageLoad(NormalMode))
          } else {
            None
          },
          if (userAnswers.get(HaveNiNumberPage).isEmpty) {
            Some(routes.HaveNiNumberController.onPageLoad(NormalMode))
          } else {
            None
          },
          if (userAnswers.get(WhatIsYourNameIndividualPage).isEmpty) {
            Some(routes.WhatIsYourNameIndividualController.onPageLoad(NormalMode))
          } else {
            None
          },
          if (userAnswers.get(RegisterDateOfBirthPage).isEmpty) {
            Some(routes.RegisterDateOfBirthController.onPageLoad(NormalMode))
          } else {
            None
          }
          // CARF-183  if (userAnswers.get(IndividualContactEmailPage).isEmpty) Some(routes.IndividualContactEmailController.onPageLoad(NormalMode)) else None
        )
      }

    val firstMissingPageRoute: Option[Call] = missingPageChecks.flatten.headOption

    firstMissingPageRoute.getOrElse {
      routes.CheckYourAnswersController.onPageLoad()
    }
  }
}
