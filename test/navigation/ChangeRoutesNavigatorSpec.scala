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

package navigation

import base.SpecBase
import controllers.routes
import models.RegistrationType.*
import models.{ChangeMode, NormalMode, ProvideMode}
import pages.*
import pages.individual.IndividualEmailPage
import pages.orgWithoutId.{HaveTradingNamePage, OrgWithoutIdBusinessNamePage, TradingNamePage}
import pages.organisation.*

class ChangeRoutesNavigatorSpec extends SpecBase {

  val navigator = new Navigator()

  "ChangeRoutesNavigator" - {
    "when on NavigatorOnlyIndividualRegistrationTypePage" - {
      "must navigate to RegisteredAddressInUkController when registration type is Sole Trader" in {
        val userAnswers = emptyUserAnswers
          .withPage(RegistrationTypePage, SoleTrader)

        navigator.nextPage(
          NavigatorOnlyIndividualRegistrationTypePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.RegisteredAddressInUkController.onPageLoad(ChangeMode)
      }
      "must navigate to HaveNiNumberController when registration type is Individual" in {
        val userAnswers = emptyUserAnswers
          .withPage(RegistrationTypePage, Individual)

        navigator.nextPage(
          NavigatorOnlyIndividualRegistrationTypePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.individual.routes.HaveNiNumberController.onPageLoad(ChangeMode)
      }
      "must navigate to JourneyRecovery when registration type is neither Sole Trader nor Individual" in {
        val userAnswers = emptyUserAnswers
          .withPage(RegistrationTypePage, LimitedCompany)

        navigator.nextPage(
          NavigatorOnlyIndividualRegistrationTypePage,
          ChangeMode,
          userAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "when on NavigatorOnlyOrganisationRegistrationTypePage" - {
      "must navigate to RegisteredAddressInUkController" in {
        navigator.nextPage(
          NavigatorOnlyOrganisationRegistrationTypePage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.RegisteredAddressInUkController.onPageLoad(ChangeMode)
      }
    }

    "when on RegisteredAddressInUkPage" - {
      "must navigate to YourUniqueTaxpayerReferenceController if answer was yes" in {
        val userAnswers = emptyUserAnswers.withPage(RegisteredAddressInUkPage, true)

        navigator.nextPage(
          RegisteredAddressInUkPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.organisation.routes.YourUniqueTaxpayerReferenceController.onPageLoad(ChangeMode)
      }

      "must navigate to HaveUTRController if answer was yes" in {
        val userAnswers = emptyUserAnswers.withPage(RegisteredAddressInUkPage, false)

        navigator.nextPage(
          RegisteredAddressInUkPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.organisation.routes.HaveUTRController.onPageLoad(ChangeMode)
      }

      "must navigate to journey recovery if answer is missing" in {
        navigator.nextPage(
          RegisteredAddressInUkPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "when on HaveUTRPage" - {
      "must navigate to YourUniqueTaxpayerReferenceController if answer was yes" in {
        val userAnswers = emptyUserAnswers.withPage(HaveUTRPage, true)

        navigator.nextPage(
          HaveUTRPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.organisation.routes.YourUniqueTaxpayerReferenceController.onPageLoad(ChangeMode)
      }

      "must navigate to HaveNiNumberController if answer was no and user is sole trader" in {
        val userAnswers = emptyUserAnswers.withPage(HaveUTRPage, false).withPage(RegistrationTypePage, SoleTrader)

        navigator.nextPage(
          HaveUTRPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.individual.routes.HaveNiNumberController.onPageLoad(ChangeMode)
      }

      "must navigate to OrgWithoutIdBusinessNameController if answer was no, user is non sole trader and OrgWithoutIdBusinessNamePage doesn't have answers" in {
        val userAnswers = emptyUserAnswers
          .withPage(HaveUTRPage, false)
          .withPage(RegistrationTypePage, LimitedCompany)

        navigator.nextPage(
          HaveUTRPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.orgWithoutId.routes.OrgWithoutIdBusinessNameController.onPageLoad(NormalMode)
      }

      "must navigate to Check Your Answers if answer was no, user is non sole trader and OrgWithoutIdBusinessNamePage has answers" in {
        val userAnswers = emptyUserAnswers
          .withPage(HaveUTRPage, false)
          .withPage(RegistrationTypePage, LimitedCompany)
          .withPage(OrgWithoutIdBusinessNamePage, "TestName")

        navigator.nextPage(
          HaveUTRPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }

      "must navigate to journey recovery if answer is missing" in {
        navigator.nextPage(
          HaveUTRPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "when on YourUtrPageForNavigatorOnly" - {
      "must navigate to RegisteredAddressInUkController when registration type is Sole Trader" in {
        val userAnswers = emptyUserAnswers
          .withPage(RegistrationTypePage, SoleTrader)

        navigator.nextPage(
          YourUtrPageForNavigatorOnly,
          ChangeMode,
          userAnswers
        ) mustBe controllers.organisation.routes.WhatIsYourNameController.onPageLoad(ChangeMode)
      }
      "must navigate to HaveNiNumberController when registration type is Individual" in {
        val userAnswers = emptyUserAnswers
          .withPage(RegistrationTypePage, Individual)

        navigator.nextPage(
          YourUtrPageForNavigatorOnly,
          ChangeMode,
          userAnswers
        ) mustBe controllers.organisation.routes.WhatIsTheNameOfYourBusinessController.onPageLoad(ChangeMode)
      }
    }

    "when on WhatIsTheNameOfYourBusinessPage" - {
      "must navigate to IsThisYourBusinessController" in {
        navigator.nextPage(
          WhatIsTheNameOfYourBusinessPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe routes.IsThisYourBusinessController.onPageLoad(ChangeMode)
      }
    }

    "when on WhatIsYourNamePage" - {
      "must navigate to IsThisYourBusinessController" in {
        navigator.nextPage(
          WhatIsYourNamePage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe routes.IsThisYourBusinessController.onPageLoad(ChangeMode)
      }
    }

    "when on IsThisYourBusinessPage" - {
      "must navigate to IndividualEmailController when answer is yes, user is sole trader and contact details have not been answered" in {
        val userAnswers = emptyUserAnswers
          .withPage(RegistrationTypePage, SoleTrader)
          .withPage(IsThisYourBusinessPage, testIsThisYourBusinessPageDetails)

        navigator.nextPage(
          IsThisYourBusinessPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode)
      }

      "must navigate to Check your answers page when answer is yes, user is sole trader and contact details have been answered" in {
        val userAnswers = emptyUserAnswers
          .withPage(RegistrationTypePage, SoleTrader)
          .withPage(IsThisYourBusinessPage, testIsThisYourBusinessPageDetails)
          .withPage(IndividualEmailPage, "testEmail")

        navigator.nextPage(
          IsThisYourBusinessPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }

      "must navigate to Check your answers page when answer is yes, user is not sole trader and contact details have been answered" in {
        val userAnswers = emptyUserAnswers
          .withPage(RegistrationTypePage, LimitedCompany)
          .withPage(IsThisYourBusinessPage, testIsThisYourBusinessPageDetails)
          .withPage(FirstContactNamePage, "Timmy")

        navigator.nextPage(
          IsThisYourBusinessPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }

      "must navigate to OrgYourContactDetailsController when answer is yes, user is not sole trader and contact details have not been answered" in {
        val userAnswers = emptyUserAnswers
          .withPage(RegistrationTypePage, LimitedCompany)
          .withPage(IsThisYourBusinessPage, testIsThisYourBusinessPageDetails)

        navigator.nextPage(
          IsThisYourBusinessPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.organisation.routes.OrgYourContactDetailsController.onPageLoad()
      }

      "must navigate to ProblemDifferentBusinessController when answer is no, user is ct automatched" in {
        val userAnswers = emptyUserAnswers
          .withPage(IsThisYourBusinessPage, testIsThisYourBusinessPageDetails.copy(pageAnswer = Some(false)))
          .copy(isCtAutoMatched = true)

        navigator.nextPage(
          IsThisYourBusinessPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.organisation.routes.ProblemDifferentBusinessController.onPageLoad()
      }

      "must navigate to ProblemSoleTraderNotIdentifiedController when answer is no and user is sole trader" in {
        val userAnswers = emptyUserAnswers
          .withPage(RegistrationTypePage, SoleTrader)
          .withPage(IsThisYourBusinessPage, testIsThisYourBusinessPageDetails.copy(pageAnswer = Some(false)))

        navigator.nextPage(
          IsThisYourBusinessPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.individual.routes.ProblemSoleTraderNotIdentifiedController.onPageLoad()
      }

      "must navigate to BusinessNotIdentifiedController when answer is no and user is not sole trader and not ct automatched" in {
        val userAnswers = emptyUserAnswers
          .withPage(RegistrationTypePage, LimitedCompany)
          .withPage(IsThisYourBusinessPage, testIsThisYourBusinessPageDetails.copy(pageAnswer = Some(false)))

        navigator.nextPage(
          IsThisYourBusinessPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.organisation.routes.BusinessNotIdentifiedController.onPageLoad()
      }

      "must navigate to Journey Recovery page when answer is empty" in {
        val userAnswers = emptyUserAnswers
          .withPage(IsThisYourBusinessPage, testIsThisYourBusinessPageDetails.copy(pageAnswer = None))

        navigator.nextPage(
          IsThisYourBusinessPage,
          ChangeMode,
          userAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "when on HaveTradingNamePage" - {
      "must navigate to TradingNameController when answer is yes and trading name has not been answered" in {
        val userAnswers = emptyUserAnswers.withPage(HaveTradingNamePage, true)

        navigator.nextPage(
          HaveTradingNamePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.orgWithoutId.routes.TradingNameController.onPageLoad(ChangeMode)
      }

      "must navigate to CheckYourAnswersController when answer is yes and trading name has already been answered" in {
        val userAnswers = emptyUserAnswers
          .withPage(HaveTradingNamePage, true)
          .withPage(TradingNamePage, "Test Trading Name")

        navigator.nextPage(
          HaveTradingNamePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }

      "must navigate to CheckYourAnswersController when answer is no" in {
        val userAnswers = emptyUserAnswers.withPage(HaveTradingNamePage, false)

        navigator.nextPage(
          HaveTradingNamePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }
    }

    "when on an unknown page" - {
      "must navigate to JourneyRecoveryController" in {
        val userAnswers = emptyUserAnswers

        navigator.nextPage(
          pages.individual.HaveNiNumberPage,
          ProvideMode,
          userAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }
  }
}
