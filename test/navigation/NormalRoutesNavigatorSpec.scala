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
import models.IndividualRegistrationType.{Individual, SoleTrader}
import models.{Address, IndividualRegistrationType, IsThisYourBusinessPageDetails, NormalMode, OrganisationRegistrationType, UniqueTaxpayerReference, UserAnswers}
import pages.*

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
      ) mustBe routes.HaveNiNumberController.onPageLoad(NormalMode)
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
      ) mustBe routes.WhatIsTheNameOfYourBusinessController.onPageLoad(NormalMode)
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
      ) mustBe routes.PlaceholderController.onPageLoad("Must redirect to /your-name (CARF-125)")
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
      ) mustBe routes.PlaceholderController.onPageLoad("Must redirect to /your-name (CARF-125)")
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
      ) mustBe routes.WhatIsTheNameOfYourBusinessController.onPageLoad(NormalMode)
    }

    "RegisteredAddressInUkPage navigation" - {

      "must go to UTR page when user answers 'Yes' to UK address" in {
        val userAnswers = UserAnswers("id").set(RegisteredAddressInUkPage, true).success.value

        navigator.nextPage(
          RegisteredAddressInUkPage,
          NormalMode,
          userAnswers
        ) mustBe routes.YourUniqueTaxpayerReferenceController.onPageLoad(NormalMode)
      }

      "must go to Have UTR page when user answers 'No' to UK address" in {
        val userAnswers = UserAnswers("id").set(RegisteredAddressInUkPage, false).success.value

        navigator.nextPage(
          RegisteredAddressInUkPage,
          NormalMode,
          userAnswers
        ) mustBe routes.HaveUTRController.onPageLoad(NormalMode)
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
        ) mustBe routes.YourUniqueTaxpayerReferenceController.onPageLoad(NormalMode)
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
        ) mustBe routes.HaveNiNumberController.onPageLoad(NormalMode)
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
        ) mustBe routes.HaveNiNumberController.onPageLoad(NormalMode)
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
        ) mustBe routes.PlaceholderController.onPageLoad(
          "redirect to - What is the name of your business? page /register/without-id/business-name (CARF-148)"
        )
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
          ) mustBe routes.NiNumberController.onPageLoad(NormalMode)
        }
      }
      "when user answers 'false' (no, I don't have a National Insurance number)" - {
        "must navigate to: What is your name?" in {
          val userAnswers = UserAnswers("id")
            .set(HaveNiNumberPage, false)
            .success
            .value

          navigator.nextPage(
            HaveNiNumberPage,
            NormalMode,
            userAnswers
          ) mustBe routes.PlaceholderController.onPageLoad("Must redirect to /without-id/name (CARF-169)")
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
        ) mustBe routes.PlaceholderController.onPageLoad("Must redirect to /register/name (CARF-165)")
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
          ) mustBe routes.PlaceholderController.onPageLoad("Must redirect to /register/individual-email (CARF-183)")
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
          ) mustBe routes.PlaceholderController.onPageLoad("Must redirect to /register/individual-email (CARF-183)")
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
          ) mustBe routes.OrgYourContactDetailsController.onPageLoad()
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
          ) mustBe routes.OrgYourContactDetailsController.onPageLoad()
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
          ) mustBe routes.PlaceholderController.onPageLoad("Must redirect to /problem/different-business (CARF-127)")
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
          ) mustBe routes.PlaceholderController.onPageLoad(
            "Must redirect to /problem/sole-trader-not-identified (CARF-129)"
          )
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
          ) mustBe routes.PlaceholderController.onPageLoad(
            "Must redirect to /problem/business-not-identified (CARF-147)"
          )
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
          ) mustBe routes.PlaceholderController.onPageLoad(
            "Must redirect to /problem/business-not-identified (CARF-147)"
          )
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
  }
}
