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
import models.{ChangeMode, Name, NormalMode, ProvideMode}
import models.{format, ChangeMode, NormalMode, ProvideMode}
import pages.*
import pages.individual.*
import pages.individualWithoutId.{IndWithoutIdDateOfBirthPage, IndWithoutNinoNamePage, WhereDoYouLivePage}
import pages.orgWithoutId.{HaveTradingNamePage, OrgWithoutIdBusinessNamePage, OrganisationBusinessAddressPage, TradingNamePage}
import pages.individualWithoutId.{IndReviewConfirmAddressPageForNavigatorOnly, IndWithoutIdAddressNonUkPage, IndWithoutIdAddressPageForNavigatorOnly, IndWithoutIdChooseAddressPage, IndWithoutIdDateOfBirthPage, IndWithoutNinoNamePage, WhereDoYouLivePage}
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

      "must navigate to JourneyRecoveryController when answer is missing" in {
        navigator.nextPage(
          HaveTradingNamePage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "when on OrgWithoutIdBusinessNamePage" - {
      "must navigate to CheckYourAnswersController" in {
        val userAnswers = emptyUserAnswers
          .withPage(OrgWithoutIdBusinessNamePage, "TestName")

        navigator.nextPage(
          OrgWithoutIdBusinessNamePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }
    }

    "when on TradingNamePage" - {
      "must navigate to CheckYourAnswersController" in {
        val userAnswers = emptyUserAnswers
          .withPage(TradingNamePage, "Test Trading Name")

        navigator.nextPage(
          TradingNamePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }
    }

    "when on FirstContactNamePage" - {
      "must navigate to CheckYourAnswersController" in {
        navigator.nextPage(
          FirstContactNamePage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }
    }

    "when on FirstContactEmailPage" - {
      "must navigate to CheckYourAnswersController" in {
        navigator.nextPage(
          FirstContactEmailPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }
    }

    "when on FirstContactPhonePage" - {
      "must navigate to FirstContactPhoneNumberController when answer is yes and phone number has not been answered" in {
        val userAnswers = emptyUserAnswers.withPage(FirstContactPhonePage, true)

        navigator.nextPage(
          FirstContactPhonePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.organisation.routes.FirstContactPhoneNumberController.onPageLoad(ChangeMode)
      }

      "must navigate to CheckYourAnswersController when answer is yes and phone number has already been answered" in {
        val userAnswers = emptyUserAnswers
          .withPage(FirstContactPhonePage, true)
          .withPage(FirstContactPhoneNumberPage, testPhone)

        navigator.nextPage(
          FirstContactPhonePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }

      "must navigate to CheckYourAnswersController when answer is no" in {
        val userAnswers = emptyUserAnswers.withPage(FirstContactPhonePage, false)

        navigator.nextPage(
          FirstContactPhonePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }
    }

    "when on FirstContactPhoneNumberPage" - {
      "must navigate to CheckYourAnswersController" in {
        navigator.nextPage(
          FirstContactPhoneNumberPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }
    }

    "when on OrganisationHaveSecondContactPage" - {
      "must navigate to OrganisationSecondContactNameController when answer is yes and second contact name has not been answered" in {
        val userAnswers = emptyUserAnswers.withPage(OrganisationHaveSecondContactPage, true)

        navigator.nextPage(
          OrganisationHaveSecondContactPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.organisation.routes.OrganisationSecondContactNameController.onPageLoad(NormalMode)
      }

      "must navigate to CheckYourAnswersController when answer is yes and second contact name has already been answered" in {
        val userAnswers = emptyUserAnswers
          .withPage(OrganisationHaveSecondContactPage, true)
          .withPage(OrganisationSecondContactNamePage, "Timmy")

        navigator.nextPage(
          OrganisationHaveSecondContactPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }

      "must navigate to CheckYourAnswersController when answer is no" in {
        val userAnswers = emptyUserAnswers.withPage(OrganisationHaveSecondContactPage, false)

        navigator.nextPage(
          OrganisationHaveSecondContactPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }
    }

    "when on OrganisationSecondContactNamePage" - {
      "must navigate to CheckYourAnswersController" in {
        navigator.nextPage(
          OrganisationSecondContactNamePage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }
    }

    "when on OrganisationSecondContactEmailPage" - {
      "must navigate to CheckYourAnswersController" in {
        navigator.nextPage(
          OrganisationSecondContactEmailPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }
    }

    "when on OrganisationSecondContactHavePhonePage" - {
      "must navigate to OrganisationSecondContactPhoneNumberController when answer is yes and phone number has not been answered" in {
        val userAnswers = emptyUserAnswers.withPage(OrganisationSecondContactHavePhonePage, true)

        navigator.nextPage(
          OrganisationSecondContactHavePhonePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.organisation.routes.OrganisationSecondContactPhoneNumberController.onPageLoad(NormalMode)
      }

      "must navigate to CheckYourAnswersController when answer is yes and phone number has already been answered" in {
        val userAnswers = emptyUserAnswers
          .withPage(OrganisationSecondContactHavePhonePage, true)
          .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)

        navigator.nextPage(
          OrganisationSecondContactHavePhonePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }

      "must navigate to CheckYourAnswersController when answer is no" in {
        val userAnswers = emptyUserAnswers.withPage(OrganisationSecondContactHavePhonePage, false)

        navigator.nextPage(
          OrganisationSecondContactHavePhonePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }
    }

    "when on OrganisationSecondContactPhoneNumberPage" - {
      "must navigate to CheckYourAnswersController" in {
        navigator.nextPage(
          OrganisationSecondContactPhoneNumberPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }
    }

    "when on IndividualEmailPage" - {
      "must navigate to CheckYourAnswersController" in {
        navigator.nextPage(
          IndividualEmailPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }
    }

    "when on IndividualHavePhonePage" - {
      "must navigate to IndividualPhoneNumberController when answer is yes and phone number has not been answered" in {
        val userAnswers = emptyUserAnswers.withPage(IndividualHavePhonePage, true)

        navigator.nextPage(
          IndividualHavePhonePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.individual.routes.IndividualPhoneNumberController.onPageLoad(NormalMode)
      }

      "must navigate to CheckYourAnswersController when answer is yes and phone number has already been answered" in {
        val userAnswers = emptyUserAnswers
          .withPage(IndividualHavePhonePage, true)
          .withPage(IndividualPhoneNumberPage, testPhone)

        navigator.nextPage(
          IndividualHavePhonePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }

      "must navigate to CheckYourAnswersController when answer is no" in {
        val userAnswers = emptyUserAnswers.withPage(IndividualHavePhonePage, false)

        navigator.nextPage(
          IndividualHavePhonePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }

      "must navigate to journey recovery when answer is missing" in {
        navigator.nextPage(
          IndividualHavePhonePage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "when on IndividualPhoneNumberPage" - {
      "must navigate to CheckYourAnswersController" in {
        navigator.nextPage(
          IndividualPhoneNumberPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }
    }

    "when on OrganisationBusinessAddressPage" - {
      "must navigate to CheckYourAnswersController when FirstContactName has been answered" in {
        val userAnswers = emptyUserAnswers
          .withPage(FirstContactNamePage, "John Doe")

        navigator.nextPage(
          OrganisationBusinessAddressPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }

      "must navigate to OrgYourContactDetailsController when FirstContactName has not been answered" in {
        val userAnswers = emptyUserAnswers

        navigator.nextPage(
          OrganisationBusinessAddressPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.organisation.routes.OrgYourContactDetailsController.onPageLoad()
      }
    }

    "when on HaveNiNumberPage" - {
      "must navigate to NiNumberController when answer is yes and NiNumber has not been answered" in {
        val userAnswers = emptyUserAnswers.withPage(HaveNiNumberPage, true)

        navigator.nextPage(
          HaveNiNumberPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.individual.routes.NiNumberController.onPageLoad(NormalMode)
      }

      "must navigate to CheckYourAnswersController when answer is yes and NiNumber has already been answered" in {
        val userAnswers = emptyUserAnswers
          .withPage(HaveNiNumberPage, true)
          .withPage(NiNumberPage, "AA123456A")

        navigator.nextPage(
          HaveNiNumberPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }

      "must navigate to IndWithoutNinoNameController when answer is no and WhatIsYourNameIndividualPage has not been answered" in {
        val userAnswers = emptyUserAnswers.withPage(HaveNiNumberPage, false)

        navigator.nextPage(
          HaveNiNumberPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.individualWithoutId.routes.IndWithoutNinoNameController.onPageLoad(NormalMode)
      }

      "must navigate to CheckYourAnswersController when answer is no and IndWithoutNinoNamePage has already been answered" in {
        val userAnswers = emptyUserAnswers
          .withPage(HaveNiNumberPage, false)
          .withPage(IndWithoutNinoNamePage, Name("Timmy", "McFly"))

        navigator.nextPage(
          HaveNiNumberPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }

      "must navigate to JourneyRecoveryController when answer is missing" in {
        navigator.nextPage(
          HaveNiNumberPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "when on NiNumberPage" - {
      "must navigate to WhatIsYourNameIndividualController" in {
        navigator.nextPage(
          NiNumberPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.individual.routes.WhatIsYourNameIndividualController.onPageLoad(ChangeMode)
      }
    }

    "when on WhatIsYourNameIndividualPage" - {
      "must navigate to RegisterDateOfBirthController" in {
        navigator.nextPage(
          WhatIsYourNameIndividualPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.individual.routes.RegisterDateOfBirthController.onPageLoad(ChangeMode)
      }
    }

    "when on RegisterDateOfBirthPage" - {
      "must navigate to RegisterDateOfBirthController" in {
        navigator.nextPage(
          RegisterDateOfBirthPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.individual.routes.RegisterIdentityConfirmedController.onPageLoad(ChangeMode)
      }
    }

    "when on IndWithoutNinoNamePage" - {
      "must navigate to CheckYourAnswersController" in {
        navigator.nextPage(
          IndWithoutNinoNamePage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }
    }

    "when on IndWithoutIdDateOfBirthPage" - {
      "must navigate to CheckYourAnswersController" in {
        navigator.nextPage(
          IndWithoutIdDateOfBirthPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }
    }

    "when on WhereDoYouLivePage" - {
      "must navigate to IndFindAddressController when answer is true" in {
        navigator.nextPage(
          WhereDoYouLivePage,
          ChangeMode,
          emptyUserAnswers.withPage(WhereDoYouLivePage, true)
        ) mustBe controllers.individualWithoutId.routes.IndFindAddressController.onPageLoad(NormalMode)
      }

      "must navigate to IndWithoutIdAddressNonUkController when answer is false" in {
        navigator.nextPage(
          WhereDoYouLivePage,
          ChangeMode,
          emptyUserAnswers.withPage(WhereDoYouLivePage, false)
        ) mustBe controllers.individualWithoutId.routes.IndWithoutIdAddressNonUkController.onPageLoad(NormalMode)
      }

      "must navigate to JourneyRecoveryController when answer cannot be found" in {
        navigator.nextPage(
          WhereDoYouLivePage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "when on IndWithoutIdAddressNonUkPage" - {
      "must navigate to CheckYourAnswersController if individual email is populated" in {
        navigator.nextPage(
          IndWithoutIdAddressNonUkPage,
          ChangeMode,
          emptyUserAnswers.withPage(IndividualEmailPage, testEmail)
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }

      "must navigate to IndividualEmailController if individual email is NOT populated" in {
        navigator.nextPage(
          IndWithoutIdAddressNonUkPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode)
      }
    }

    "when on IndWithoutIdChooseAddressPage" - {
      "must navigate to CheckYourAnswersController if individual email is populated and an address has been chosen" in {
        navigator.nextPage(
          IndWithoutIdChooseAddressPage,
          ChangeMode,
          emptyUserAnswers
            .withPage(IndWithoutIdChooseAddressPage, testAddressUk.format)
            .withPage(IndividualEmailPage, testEmail)
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }

      "must navigate to IndividualEmailController if individual email is NOT populated and an address has been chosen" in {
        navigator.nextPage(
          IndWithoutIdChooseAddressPage,
          ChangeMode,
          emptyUserAnswers.withPage(IndWithoutIdChooseAddressPage, testAddressUk.format)
        ) mustBe controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode)
      }

      "must navigate to IndWithoutIdAddressController if no address has been chosen" in {
        navigator.nextPage(
          IndWithoutIdChooseAddressPage,
          ChangeMode,
          emptyUserAnswers.withPage(IndWithoutIdChooseAddressPage, "none")
        ) mustBe controllers.individualWithoutId.routes.IndWithoutIdAddressController.onPageLoad(NormalMode)
      }

      "must navigate to Journey Recover if user answers is empty" in {
        navigator.nextPage(
          IndWithoutIdChooseAddressPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "when on IndReviewConfirmAddressPage" - {
      "must navigate to CheckYourAnswersController if individual email is populated" in {
        navigator.nextPage(
          IndReviewConfirmAddressPageForNavigatorOnly,
          ChangeMode,
          emptyUserAnswers.withPage(IndividualEmailPage, testEmail)
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }

      "must navigate to IndividualEmailController if individual email is NOT populated" in {
        navigator.nextPage(
          IndReviewConfirmAddressPageForNavigatorOnly,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode)
      }
    }

    "when on IndWithoutIdAddressPage" - {
      "must navigate to CheckYourAnswersController if individual email is populated" in {
        navigator.nextPage(
          IndWithoutIdAddressPageForNavigatorOnly,
          ChangeMode,
          emptyUserAnswers.withPage(IndividualEmailPage, testEmail)
        ) mustBe controllers.routes.CheckYourAnswersController.onPageLoad()
      }

      "must navigate to IndividualEmailController if individual email is NOT populated" in {
        navigator.nextPage(
          IndWithoutIdAddressPageForNavigatorOnly,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode)
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
