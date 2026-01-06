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

import base.SpecBase
import controllers.routes
import models.*
import models.IndividualRegistrationType.{Individual, SoleTrader}
import org.scalactic.Prettifier.default
import pages.*
import pages.individual.*
import pages.individualWithoutId.IndWithoutNinoNamePage
import pages.orgWithoutId.{HaveTradingNamePage, OrgWithoutIdBusinessNamePage, OrganisationBusinessAddressPage}
import pages.organisation.*
import play.api.libs.json.Json

import java.time.LocalDate

class NormalRoutesNavigatorSpec extends SpecBase {

  val navigator = new Navigator

  "NormalRoutesNavigator" - {

    "must go from a page that doesn't exist in the route map to Journey Recovery" in {

      case object UnknownPage extends Page
      navigator.nextPage(UnknownPage, NormalMode, UserAnswers("id")) mustBe routes.JourneyRecoveryController
        .onPageLoad()
    }

    "must go from OrganisationRegistrationTypePage to Registered Address in the UK page" in {

      navigator.nextPage(
        OrganisationRegistrationTypePage,
        NormalMode,
        UserAnswers("id")
      ) mustBe routes.RegisteredAddressInUkController.onPageLoad(NormalMode)
    }

    "must go from IndividualRegistrationTypePage to Registered Address in the UK Page when user is a Sole Trader" in {
      val userAnswers = UserAnswers("id").set(IndividualRegistrationTypePage, SoleTrader).success.value

      navigator.nextPage(
        IndividualRegistrationTypePage,
        NormalMode,
        userAnswers
      ) mustBe routes.RegisteredAddressInUkController.onPageLoad(NormalMode)
    }

    "must go from IndividualRegistrationTypePage to Do You Have An NI Number Page? when user is an Individual" in {
      val userAnswers = UserAnswers("id").set(IndividualRegistrationTypePage, Individual).success.value

      navigator.nextPage(
        IndividualRegistrationTypePage,
        NormalMode,
        userAnswers
      ) mustBe controllers.individual.routes.HaveNiNumberController.onPageLoad(NormalMode)
    }

    "must go from YourUniqueTaxpayerReferencePage to What is the registered name of your business for non soleTrader" in {

      val updatedAnswers =
        emptyUserAnswers
          .set(OrganisationRegistrationTypePage, OrganisationRegistrationType.LimitedCompany)
          .success
          .value
          .set(YourUniqueTaxpayerReferencePage, UniqueTaxpayerReference("1234567890"))
          .success
          .value

      navigator.nextPage(
        YourUniqueTaxpayerReferencePage,
        NormalMode,
        updatedAnswers
      ) mustBe controllers.organisation.routes.WhatIsTheNameOfYourBusinessController.onPageLoad(NormalMode)
    }

    "must go from YourUniqueTaxpayerReferencePage to What is your name page for soleTrader as an organisation" in {

      val updatedAnswers =
        emptyUserAnswers
          .set(OrganisationRegistrationTypePage, OrganisationRegistrationType.SoleTrader)
          .success
          .value
          .set(YourUniqueTaxpayerReferencePage, UniqueTaxpayerReference("1234567890"))
          .success
          .value

      navigator.nextPage(
        YourUniqueTaxpayerReferencePage,
        NormalMode,
        updatedAnswers
      ) mustBe controllers.organisation.routes.WhatIsYourNameController.onPageLoad(NormalMode)
    }

    "must go from YourUniqueTaxpayerReferencePage to What is your name page for soleTrader as an individual" in {

      val updatedAnswers =
        emptyUserAnswers
          .set(IndividualRegistrationTypePage, IndividualRegistrationType.SoleTrader)
          .success
          .value
          .set(YourUniqueTaxpayerReferencePage, UniqueTaxpayerReference("1234567890"))
          .success
          .value

      navigator.nextPage(
        YourUniqueTaxpayerReferencePage,
        NormalMode,
        updatedAnswers
      ) mustBe controllers.organisation.routes.WhatIsYourNameController.onPageLoad(NormalMode)
    }

    "must go from YourUniqueTaxpayerReferencePage to What is your business name page for anything other than soleTrader" in {

      val updatedAnswers =
        emptyUserAnswers
          .set(OrganisationRegistrationTypePage, OrganisationRegistrationType.LLP)
          .success
          .value
          .set(YourUniqueTaxpayerReferencePage, UniqueTaxpayerReference("1234567890"))
          .success
          .value

      navigator.nextPage(
        YourUniqueTaxpayerReferencePage,
        NormalMode,
        updatedAnswers
      ) mustBe controllers.organisation.routes.WhatIsTheNameOfYourBusinessController.onPageLoad(NormalMode)
    }

    "RegisteredAddressInUkPage navigation" - {

      "must go to UTR page when user answers 'Yes' to UK address" in {
        val userAnswers = UserAnswers("id").set(RegisteredAddressInUkPage, true).success.value

        navigator.nextPage(
          RegisteredAddressInUkPage,
          NormalMode,
          userAnswers
        ) mustBe controllers.organisation.routes.YourUniqueTaxpayerReferenceController.onPageLoad(NormalMode)
      }

      "must go to Have UTR page when user answers 'No' to UK address" in {
        val userAnswers = UserAnswers("id").set(RegisteredAddressInUkPage, false).success.value

        navigator.nextPage(
          RegisteredAddressInUkPage,
          NormalMode,
          userAnswers
        ) mustBe controllers.organisation.routes.HaveUTRController.onPageLoad(NormalMode)
      }

      "must go to Journey Recovery when no answer is provided" in {
        val userAnswers = UserAnswers("id")

        navigator.nextPage(
          RegisteredAddressInUkPage,
          NormalMode,
          userAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "HaveUTRPage navigation" - {

      "must go to UTR page when user answers 'Yes' to having UTR" in {
        val userAnswers = UserAnswers("id").set(HaveUTRPage, true).success.value

        navigator.nextPage(
          HaveUTRPage,
          NormalMode,
          userAnswers
        ) mustBe controllers.organisation.routes.YourUniqueTaxpayerReferenceController.onPageLoad(NormalMode)
      }

      "must go to Have NI Number page when user answers 'No' to having UTR and is Organisation SoleTrader" in {
        val userAnswers = UserAnswers("id")
          .set(HaveUTRPage, false)
          .success
          .value
          .set(OrganisationRegistrationTypePage, OrganisationRegistrationType.SoleTrader)
          .success
          .value

        navigator.nextPage(
          HaveUTRPage,
          NormalMode,
          userAnswers
        ) mustBe controllers.individual.routes.HaveNiNumberController.onPageLoad(NormalMode)
      }

      "must go to Have NI Number page when user answers 'No' to having UTR and is Individual SoleTrader" in {
        val userAnswers = UserAnswers("id")
          .set(HaveUTRPage, false)
          .success
          .value
          .set(IndividualRegistrationTypePage, IndividualRegistrationType.SoleTrader)
          .success
          .value

        navigator.nextPage(
          HaveUTRPage,
          NormalMode,
          userAnswers
        ) mustBe controllers.individual.routes.HaveNiNumberController.onPageLoad(NormalMode)
      }

      "must go to Business Name page when user answers 'No' to having UTR and is Organisation" in {
        val userAnswers = UserAnswers("id")
          .set(HaveUTRPage, false)
          .success
          .value
          .set(OrganisationRegistrationTypePage, OrganisationRegistrationType.LimitedCompany)
          .success
          .value

        navigator.nextPage(
          HaveUTRPage,
          NormalMode,
          userAnswers
        ) mustBe controllers.orgWithoutId.routes.OrgWithoutIdBusinessNameController.onPageLoad(NormalMode)
      }

      "must go to Journey Recovery when user answers 'No' to having UTR but no registration type is set" in {
        val userAnswers = UserAnswers("id").set(HaveUTRPage, false).success.value

        navigator.nextPage(
          HaveUTRPage,
          NormalMode,
          userAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }

      "must go to Journey Recovery when no answer is provided for HaveUTR" in {
        val userAnswers = UserAnswers("id")

        navigator.nextPage(
          HaveUTRPage,
          NormalMode,
          userAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "HaveNiNumberPage navigation" - {
      "when user answers 'true' (yes, I have a National Insurance number)" - {
        "must navigate to: What is your National Insurance number?" in {
          val userAnswers = UserAnswers("id")
            .set(HaveNiNumberPage, true)
            .success
            .value

          navigator.nextPage(
            HaveNiNumberPage,
            NormalMode,
            userAnswers
          ) mustBe controllers.individual.routes.NiNumberController.onPageLoad(NormalMode)
        }
      }
      "when user answers 'false' (no, I don't have a National Insurance number)" - {
        "must navigate to: Individual or Sole Trader without NINO: What is your name?" in {
          val userAnswers = UserAnswers("id")
            .set(HaveNiNumberPage, false)
            .success
            .value

          navigator.nextPage(
            HaveNiNumberPage,
            NormalMode,
            userAnswers
          ) mustBe controllers.individualWithoutId.routes.IndWithoutNinoNameController.onPageLoad(NormalMode)
        }
      }
    }

    "NiNumberPage navigation" - {
      "must navigate to name page after NI number is provided" in {
        val userAnswers = UserAnswers("id")
          .set(NiNumberPage, "BA123456A")
          .success
          .value

        navigator.nextPage(
          NiNumberPage,
          NormalMode,
          userAnswers
        ) mustBe controllers.individual.routes.WhatIsYourNameIndividualController.onPageLoad(NormalMode)
      }
    }

    "IsThisYourBusinessPage navigation" - {

      "when user answers 'true' (yes, this is their business)" - {

        "must navigate to individual email page for organisation sole traders" in {
          val userAnswers = UserAnswers("id")
            .set(
              IsThisYourBusinessPage,
              IsThisYourBusinessPageDetails(
                "Test Business",
                Address("Test Line 1", None, None, None, None, "GB"),
                Some(true)
              )
            )
            .success
            .value
            .set(OrganisationRegistrationTypePage, OrganisationRegistrationType.SoleTrader)
            .success
            .value
          navigator.nextPage(
            IsThisYourBusinessPage,
            NormalMode,
            userAnswers
          ) mustBe controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode)
        }

        "must navigate to individual email page for individual sole traders" in {
          val userAnswers = UserAnswers("id")
            .set(
              IsThisYourBusinessPage,
              IsThisYourBusinessPageDetails(
                "Test Business",
                Address("Test Line 1", None, None, None, None, "GB"),
                Some(true)
              )
            )
            .success
            .value
            .set(IndividualRegistrationTypePage, IndividualRegistrationType.SoleTrader)
            .success
            .value
          navigator.nextPage(
            IsThisYourBusinessPage,
            NormalMode,
            userAnswers
          ) mustBe controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode)
        }

        "must navigate to contact details page for non-sole traders" in {
          val userAnswers = UserAnswers("id")
            .set(
              IsThisYourBusinessPage,
              IsThisYourBusinessPageDetails(
                "Test Business",
                Address("Test Line 1", None, None, None, None, "GB"),
                Some(true)
              )
            )
            .success
            .value
            .set(OrganisationRegistrationTypePage, OrganisationRegistrationType.LimitedCompany)
            .success
            .value

          navigator.nextPage(
            IsThisYourBusinessPage,
            NormalMode,
            userAnswers
          ) mustBe controllers.organisation.routes.OrgYourContactDetailsController.onPageLoad()
        }

        "must navigate to contact details page when no organisation type is set" in {
          val userAnswers = UserAnswers("id")
            .set(
              IsThisYourBusinessPage,
              IsThisYourBusinessPageDetails(
                "Test Business",
                Address("Test Line 1", None, None, None, None, "GB"),
                Some(true)
              )
            )
            .success
            .value

          navigator.nextPage(
            IsThisYourBusinessPage,
            NormalMode,
            userAnswers
          ) mustBe controllers.organisation.routes.OrgYourContactDetailsController.onPageLoad()
        }
      }

      "when user answers 'false' (no, this is not their business)" - {

        "must navigate to different business page when CT auto-matched" in {

          val testUtr = UniqueTaxpayerReference("1234567890")

          val userAnswers = UserAnswers("id")
            .set(
              IsThisYourBusinessPage,
              IsThisYourBusinessPageDetails(
                "Test Business",
                Address("Test Line 1", None, None, None, None, "GB"),
                Some(false)
              )
            )
            .success
            .value
            .set(IndexPage, testUtr)
            .success
            .value

          navigator.nextPage(
            IsThisYourBusinessPage,
            NormalMode,
            userAnswers
          ) mustBe controllers.organisation.routes.ProblemDifferentBusinessController.onPageLoad()
        }

        "must navigate to sole trader not identified page when not CT auto-matched and is sole trader" in {
          val userAnswers = UserAnswers("id")
            .set(
              IsThisYourBusinessPage,
              IsThisYourBusinessPageDetails(
                "Test Business",
                Address("Test Line 1", None, None, None, None, "GB"),
                Some(false)
              )
            )
            .success
            .value
            .set(OrganisationRegistrationTypePage, OrganisationRegistrationType.SoleTrader)
            .success
            .value

          navigator.nextPage(
            IsThisYourBusinessPage,
            NormalMode,
            userAnswers
          ) mustBe controllers.individual.routes.ProblemSoleTraderNotIdentifiedController.onPageLoad()
        }

        "must navigate to business not identified page when not CT auto-matched and not sole trader" in {
          val userAnswers = UserAnswers("id")
            .set(
              IsThisYourBusinessPage,
              IsThisYourBusinessPageDetails(
                "Test Business",
                Address("Test Line 1", None, None, None, None, "GB"),
                Some(false)
              )
            )
            .success
            .value
            .set(OrganisationRegistrationTypePage, OrganisationRegistrationType.LimitedCompany)
            .success
            .value

          navigator.nextPage(
            IsThisYourBusinessPage,
            NormalMode,
            userAnswers
          ) mustBe controllers.organisation.routes.BusinessNotIdentifiedController.onPageLoad()
        }

        "must navigate to business not identified page when not CT auto-matched and no organisation type" in {
          val userAnswers = UserAnswers("id")
            .set(
              IsThisYourBusinessPage,
              IsThisYourBusinessPageDetails(
                "Test Business",
                Address("Test Line 1", None, None, None, None, "GB"),
                Some(false)
              )
            )
            .success
            .value

          navigator.nextPage(
            IsThisYourBusinessPage,
            NormalMode,
            userAnswers
          ) mustBe controllers.organisation.routes.BusinessNotIdentifiedController.onPageLoad()
        }
      }

      "must navigate to Journey Recovery when no answer is provided" in {
        val userAnswers = UserAnswers("id")

        navigator.nextPage(
          IsThisYourBusinessPage,
          NormalMode,
          userAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "must go from OrgWithoutIdBusinessNamePage to Does your business trade under a different name?" in {
      val updatedAnswers =
        emptyUserAnswers
          .set(OrganisationRegistrationTypePage, OrganisationRegistrationType.LLP)
          .success
          .value
          .set(OrgWithoutIdBusinessNamePage, "valid org name")
          .success
          .value

      navigator.nextPage(
        OrgWithoutIdBusinessNamePage,
        NormalMode,
        updatedAnswers
      ) mustBe controllers.orgWithoutId.routes.HaveTradingNameController.onPageLoad(NormalMode)
    }
    "must navigate from WhatIsTheNameOfYourBusiness to IsThisYourBusiness page" in {

      val updatedAnswers =
        emptyUserAnswers
          .set(OrganisationRegistrationTypePage, OrganisationRegistrationType.LimitedCompany)
          .success
          .value
          .set(YourUniqueTaxpayerReferencePage, UniqueTaxpayerReference("1234567890"))
          .success
          .value

      navigator.nextPage(
        WhatIsTheNameOfYourBusinessPage,
        NormalMode,
        updatedAnswers
      ) mustBe routes.IsThisYourBusinessController.onPageLoad(NormalMode)
    }
    "must navigate from HaveTradingNamePage to" - {
      "business without id trading name, when Yes is selected" in {
        val updatedAnswers =
          emptyUserAnswers
            .set(HaveTradingNamePage, true)
            .success
            .value

        navigator.nextPage(
          HaveTradingNamePage,
          NormalMode,
          updatedAnswers
        ) mustBe controllers.orgWithoutId.routes.TradingNameController.onPageLoad(NormalMode)

      }
      "business without id business address, when No is selected" in {
        val updatedAnswers =
          emptyUserAnswers
            .set(HaveTradingNamePage, false)
            .success
            .value

        navigator.nextPage(
          HaveTradingNamePage,
          NormalMode,
          updatedAnswers
        ) mustBe controllers.orgWithoutId.routes.OrganisationBusinessAddressController.onPageLoad(NormalMode)
      }
    }

    "must navigate from WhatIsYourName to IsThisYourBusiness page for matched Individual SoleTrader" in {

      val updatedAnswers =
        emptyUserAnswers
          .set(IndividualRegistrationTypePage, IndividualRegistrationType.SoleTrader)
          .success
          .value
          .set(YourUniqueTaxpayerReferencePage, UniqueTaxpayerReference("1234567890"))
          .success
          .value

      navigator.nextPage(
        WhatIsYourNamePage,
        NormalMode,
        updatedAnswers
      ) mustBe routes.IsThisYourBusinessController.onPageLoad(NormalMode)
    }

    "must navigate from WhatIsYourName to IsThisYourBusiness page for Organisation SoleTrader" in {

      val updatedAnswers =
        emptyUserAnswers
          .set(OrganisationRegistrationTypePage, OrganisationRegistrationType.SoleTrader)
          .success
          .value
          .set(YourUniqueTaxpayerReferencePage, UniqueTaxpayerReference("1234567890"))
          .success
          .value

      navigator.nextPage(
        WhatIsYourNamePage,
        NormalMode,
        updatedAnswers
      ) mustBe routes.IsThisYourBusinessController.onPageLoad(NormalMode)
    }

    "must go from WhatIsYourNamePageIndividual page to WhatIsYourDateOfBirth page" in {

      val updatedAnswers =
        emptyUserAnswers
          .set(OrganisationRegistrationTypePage, OrganisationRegistrationType.SoleTrader)
          .success
          .value

      navigator.nextPage(
        WhatIsYourNameIndividualPage,
        NormalMode,
        updatedAnswers
      ) mustBe controllers.individual.routes.RegisterDateOfBirthController.onPageLoad(NormalMode)
    }

    "must go from FirstContactName page to FirstContactEmail page" in {

      val updatedAnswers =
        emptyUserAnswers
          .set(FirstContactNamePage, "Graham")
          .success
          .value

      navigator.nextPage(
        FirstContactNamePage,
        NormalMode,
        updatedAnswers
      ) mustBe controllers.organisation.routes.FirstContactEmailController.onPageLoad(NormalMode)
    }

    "must go from FirstContactEmail page to HavePhone page" in {

      val updatedAnswers =
        emptyUserAnswers
          .set(FirstContactEmailPage, "an@email.com")
          .success
          .value

      navigator.nextPage(
        FirstContactEmailPage,
        NormalMode,
        updatedAnswers
      ) mustBe controllers.organisation.routes.FirstContactPhoneController.onPageLoad(NormalMode)
    }

    "must go from OrganisationSecondContactEmail page to SecondContactHavePhone page" in {

      val updatedAnswers =
        emptyUserAnswers
          .set(FirstContactEmailPage, "an@email.com")
          .success
          .value

      navigator.nextPage(
        OrganisationSecondContactEmailPage,
        NormalMode,
        updatedAnswers
      ) mustBe controllers.organisation.routes.OrganisationSecondContactHavePhoneController.onPageLoad(NormalMode)
    }

    "OrganisationBusinessAddressPage navigation" - {

      "must navigate from OrganisationBusinessAddressPage to the next page in the journey" in {

        val userAnswers = emptyUserAnswers
          .set(
            OrganisationBusinessAddressPage,
            OrganisationBusinessAddress(
              "Address Line 1",
              Some("Address Line 2"),
              "City",
              Some("Region"),
              Some("Postcode"),
              Country("FR", "France")
            )
          )
          .success
          .value

        navigator.nextPage(
          OrganisationBusinessAddressPage,
          NormalMode,
          userAnswers
        ) mustBe controllers.organisation.routes.OrgYourContactDetailsController.onPageLoad()
      }
    }
    "RegisterDateOfBirth navigation" - {
      "must navigate from RegisterDateOfBirth to RegisterIdentityConfirmed for SoleTrader without UTR and valid IndividualDetails" in {
        val userAnswers =
          emptyUserAnswers
            .set(RegisterDateOfBirthPage, LocalDate.of(2000, 1, 1))
            .success
            .value
        navigator.nextPage(
          RegisterDateOfBirthPage,
          NormalMode,
          userAnswers
        ) mustBe controllers.individual.routes.RegisterIdentityConfirmedController.onPageLoad()
      }

      "must navigate to Journey Recovery when no answer is provided" in {
        val userAnswers = UserAnswers("id")
        navigator.nextPage(
          RegisterDateOfBirthPage,
          NormalMode,
          userAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "IndividualEmail navigation" - {
      "must navigate from IndividualEmailPage to IndividualHavePhone for Individual or SoleTrader with NINO" in {
        val userAnswers =
          emptyUserAnswers
            .set(IndividualEmailPage, "an@email.com")
            .success
            .value
        navigator.nextPage(
          IndividualEmailPage,
          NormalMode,
          userAnswers
        ) mustBe controllers.individual.routes.IndividualHavePhoneController.onPageLoad(NormalMode)
      }
    }

    "IndividualHavePhonePage navigation" - {

      "must go to Individual Phone Number page when user answers 'Yes'" in {
        val userAnswers = emptyUserAnswers.set(IndividualHavePhonePage, true).success.value

        navigator.nextPage(
          IndividualHavePhonePage,
          NormalMode,
          userAnswers
        ) mustBe controllers.individual.routes.IndividualPhoneNumberController.onPageLoad(NormalMode)
      }

      "must go to Check Your Answers page when user answers 'No'" in {
        val userAnswers = emptyUserAnswers.set(IndividualHavePhonePage, false).success.value

        navigator.nextPage(
          IndividualHavePhonePage,
          NormalMode,
          userAnswers
        ) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go to Journey Recovery when no answer is given" in {
        val userAnswers = emptyUserAnswers

        navigator.nextPage(
          IndividualHavePhonePage,
          NormalMode,
          userAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "IndividualPhoneNumberPage navigation" - {
      "must go to Check Your Answers page" in {
        val userAnswers = emptyUserAnswers.set(IndividualPhoneNumberPage, "07777777777").success.value

        navigator.nextPage(
          IndividualPhoneNumberPage,
          NormalMode,
          userAnswers
        ) mustBe routes.PlaceholderController.onPageLoad(
          "Must redirect to /register/check-answers (CARF-258)"
        )
      }
    }

    "must navigate from FirstContactPhone page to FirstContactPhoneNumber page when answer is true" in {
      val userAnswers = emptyUserAnswers
        .set(FirstContactPhonePage, true)
        .success
        .value

      navigator.nextPage(
        FirstContactPhonePage,
        NormalMode,
        userAnswers
      ) mustBe controllers.organisation.routes.FirstContactPhoneNumberController.onPageLoad(NormalMode)
    }

    "must navigate from FirstContactPhoneNumber page (/have-phone) to HaveSecondPhoneOrganisation if answer is No" in {

      val updatedAnswers =
        emptyUserAnswers
          .set(FirstContactNamePage, "John Doe")
          .success
          .value

      navigator.nextPage(
        FirstContactPhonePage,
        NormalMode,
        updatedAnswers
      ) mustBe controllers.organisation.routes.OrganisationHaveSecondContactController.onPageLoad(NormalMode)
    }

    "must navigate from FirstContactPhone page (/phone) to OrganisationHaveSecondContact page" in {

      val updatedAnswers =
        emptyUserAnswers
          .set(FirstContactPhonePage, true)
          .success
          .value
          .set(FirstContactNamePage, "John Doe")
          .success
          .value

      navigator.nextPage(
        FirstContactPhoneNumberPage,
        NormalMode,
        updatedAnswers
      ) mustBe controllers.organisation.routes.OrganisationHaveSecondContactController.onPageLoad(NormalMode)
    }

    "must navigate from OrganisationHaveSecondContact page to CheckYourAnswers when the provided answer is No" in {

      val userAnswers =
        emptyUserAnswers
          .set(OrganisationHaveSecondContactPage, false)
          .success
          .value

      navigator.nextPage(
        OrganisationHaveSecondContactPage,
        NormalMode,
        userAnswers
      ) mustBe routes.CheckYourAnswersController.onPageLoad()
    }

    "must navigate from OrganisationHaveSecondContact page to OrganisationSecondContactName page when the provided answer is Yes" in {

      val updatedAnswers =
        emptyUserAnswers
          .set(OrganisationHaveSecondContactPage, true)
          .success
          .value

      navigator.nextPage(
        OrganisationHaveSecondContactPage,
        NormalMode,
        updatedAnswers
      ) mustBe controllers.organisation.routes.OrganisationSecondContactNameController.onPageLoad(NormalMode)
    }

    "must navigate from OrganisationSecondContactName page to OrganisationSecondContactEmail page when continue is clicked" in {

      val updatedAnswers =
        emptyUserAnswers
          .set(OrganisationSecondContactNamePage, "name")
          .success
          .value

      navigator.nextPage(
        OrganisationSecondContactNamePage,
        NormalMode,
        updatedAnswers
      ) mustBe controllers.organisation.routes.OrganisationSecondContactEmailController.onPageLoad(NormalMode)

    }

    "must navigate from OrganisationHaveSecondContact page to Journey Recovery when no answer exists" in {
      val userAnswers = emptyUserAnswers

      navigator.nextPage(
        OrganisationHaveSecondContactPage,
        NormalMode,
        userAnswers
      ) mustBe routes.JourneyRecoveryController.onPageLoad()
    }

    "OrganisationSecondContactHavePhone navigation" - {
      "must go to Organisation Second Contact Phone Number page when user answers 'Yes'" in {
        val userAnswers = emptyUserAnswers.set(OrganisationSecondContactHavePhonePage, true).success.value
        navigator.nextPage(
          OrganisationSecondContactHavePhonePage,
          NormalMode,
          userAnswers
        ) mustBe controllers.organisation.routes.OrganisationSecondContactPhoneNumberController.onPageLoad(NormalMode)
      }

      "must go to Check Your Answers page when user answers 'No'" in {
        val userAnswers = emptyUserAnswers.set(OrganisationSecondContactHavePhonePage, false).success.value
        navigator.nextPage(
          OrganisationSecondContactHavePhonePage,
          NormalMode,
          userAnswers
        ) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go to Journey Recovery when no answer is given" in {
        val userAnswers = emptyUserAnswers
        navigator.nextPage(
          IndividualHavePhonePage,
          NormalMode,
          userAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "IndWithoutNinoName navigation" - {
      "must navigate from IndWithoutNinoName to IndWithoutNinoDateOfBirth for Individual or Sole Trader Without NINO  and valid name" in {
        val userAnswers =
          emptyUserAnswers
            .set(IndWithoutNinoNamePage, Name("givenName example", "familyName example"))
            .success
            .value
        navigator.nextPage(
          IndWithoutNinoNamePage,
          NormalMode,
          userAnswers
        ) mustBe routes.PlaceholderController.onPageLoad(
          "Must redirect to /register/individual-without-id/date-of-birth (CARF-170)"
        )
      }
    }

    "FirstContactPhoneNumber navigation" - {
      "must navigate from FirstContactPhoneNumberPage to OrganisationHaveSecondContactPage always" in {
        navigator.nextPage(
          FirstContactPhoneNumberPage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.organisation.routes.OrganisationHaveSecondContactController.onPageLoad(NormalMode)
      }
    }

    "OrganisationSecondContactPhoneNumber navigation" - {
      "must navigate from OrganisationSecondContactPhoneNumberPage to the Check your answers page always" in {
        navigator.nextPage(
          OrganisationSecondContactPhoneNumberPage,
          NormalMode,
          emptyUserAnswers
        ) mustBe routes.PlaceholderController.onPageLoad(
          "Must redirect to /register/check-answers (CARF-258)"
        )
      }
    }
  }
}
