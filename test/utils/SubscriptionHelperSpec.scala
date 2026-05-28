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

package utils

import base.SpecBase
import models.*
import models.JourneyType.*
import models.countries.CountryUk
import models.requests.{SubscriptionContactDetails, SubscriptionIndividualContact, SubscriptionOrganisationContact, SubscriptionRequest}
import models.responses.AddressRegistrationResponse
import pages.*
import pages.individual.*
import pages.individualWithoutId.{IndFindAddressPage, IndWithoutIdAddressPagePrePop, IndWithoutNinoNamePage}
import pages.orgWithoutId.{HaveTradingNamePage, OrgWithoutIdBusinessNamePage, TradingNamePage}
import pages.organisation.*

class SubscriptionHelperSpec extends SpecBase {

  val subscriptionHelper = new SubscriptionHelper()

  val exampleSafeId                     = SafeId("XE0000123456789")
  val testIndividualName: Name          = Name("John", "Doe")
  val testIndividualEmail               = "john.doe@example.com"
  val testIndividualPhone               = "01234567890"
  val testOrganisationFirstContactName  = "Jane Smith"
  val testOrganisationFirstEmail        = "jane.smith@example.com"
  val testOrganisationFirstPhone        = "09876543210"
  val testOrganisationSecondContactName = "Joe Bloggs"
  val testOrganisationSecondEmail       = "bob.johnson@example.com"
  val testOrganisationSecondPhone       = "01122334455"
  val testTradingName                   = "Test Trading Ltd"
  val testBusinessName                  = "Test Business Ltd"
  val testNino                          = "AB123456C"
  val testUtrValue                      = "1234567890"

  val testIndWithoutIdAddress: AddressUk = AddressUk(
    addressLine1 = "123 Test Street",
    addressLine2 = Some("Testington"),
    addressLine3 = None,
    townOrCity = "Townshire",
    postCode = "12345",
    countryUk = CountryUk("UK", "United Kingdom")
  )

  val testIndWithoutIdAddressGb: AddressUk = testIndWithoutIdAddress.copy(countryUk = CountryUk("GB", "United Kingdom"))
  val testIndFindAddress: IndFindAddress   = IndFindAddress("SW1A 1AA", Some("10"))

  "SubscriptionHelper" - {

    "buildSubscriptionRequest" - {

      "for Individual with NINO journey" - {
        "should build subscription request successfully with all required fields" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithNino))
            .copy(safeId = Some(exampleSafeId))
            .withPage(WhatIsYourNameIndividualPage, testIndividualName)
            .withPage(IndividualEmailPage, testIndividualEmail)
            .withPage(IndividualHavePhonePage, true)
            .withPage(IndividualPhoneNumberPage, testIndividualPhone)
            .withPage(NiNumberPage, testNino)

          val result = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result mustBe defined
          val request = result.get

          request.idType                      mustBe "SAFE"
          request.idNumber                    mustBe exampleSafeId.value
          request.tradingName                 mustBe None
          request.gbUser                      mustBe true
          request.primaryContact.individual   mustBe Some(
            SubscriptionIndividualContact(testIndividualName.firstName, testIndividualName.lastName)
          )
          request.primaryContact.organisation mustBe None
          request.primaryContact.email        mustBe testIndividualEmail
          request.primaryContact.phone        mustBe Some(testIndividualPhone)
          request.secondaryContact            mustBe None
        }

        "should build subscription request without optional phone number" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithNino))
            .copy(safeId = Some(exampleSafeId))
            .withPage(WhatIsYourNameIndividualPage, testIndividualName)
            .withPage(IndividualEmailPage, testIndividualEmail)
            .withPage(IndividualHavePhonePage, false)
            .withPage(NiNumberPage, testNino)

          val result: Option[SubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result                          mustBe defined
          result.get.primaryContact.phone mustBe None
        }

        "should return None when name is missing" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithNino))
            .copy(safeId = Some(exampleSafeId))
            .withPage(IndividualEmailPage, testIndividualEmail)
            .withPage(NiNumberPage, testNino)

          val result: Option[SubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result mustBe None
        }

        "should return None when email is missing" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithNino))
            .copy(safeId = Some(exampleSafeId))
            .withPage(WhatIsYourNameIndividualPage, testIndividualName)
            .withPage(NiNumberPage, testNino)

          val result: Option[SubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result mustBe None
        }
      }

      "for Individual without ID journey" - {
        "should build subscription request successfully" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithoutId))
            .copy(safeId = Some(exampleSafeId))
            .withPage(IndWithoutNinoNamePage, testIndividualName)
            .withPage(IndividualEmailPage, testIndividualEmail)
            .withPage(IndividualHavePhonePage, true)
            .withPage(IndividualPhoneNumberPage, testIndividualPhone)
            .withPage(IndWithoutIdAddressPagePrePop, testIndWithoutIdAddress)

          val result: Option[SubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result mustBe defined
          val request = result.get

          request.idType                      mustBe "SAFE"
          request.idNumber                    mustBe exampleSafeId.value
          request.tradingName                 mustBe None
          request.gbUser                      mustBe false
          request.primaryContact.individual   mustBe Some(
            SubscriptionIndividualContact(testIndividualName.firstName, testIndividualName.lastName)
          )
          request.primaryContact.organisation mustBe None
          request.primaryContact.email        mustBe testIndividualEmail
          request.primaryContact.phone        mustBe Some(testIndividualPhone)
          request.secondaryContact            mustBe None
        }

        "should identify GB user when address is GB" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithoutId))
            .copy(safeId = Some(exampleSafeId))
            .withPage(IndWithoutNinoNamePage, testIndividualName)
            .withPage(IndividualEmailPage, testIndividualEmail)
            .withPage(IndividualHavePhonePage, false)
            .withPage(IndWithoutIdAddressPagePrePop, testIndWithoutIdAddressGb)

          val result: Option[SubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result            mustBe defined
          result.get.gbUser mustBe true
        }

        "should identify GB user when address lookup is present" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithoutId))
            .copy(safeId = Some(exampleSafeId))
            .withPage(IndWithoutNinoNamePage, testIndividualName)
            .withPage(IndividualHavePhonePage, false)
            .withPage(IndividualEmailPage, testIndividualEmail)
            .withPage(IndFindAddressPage, testIndFindAddress)

          val result: Option[SubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result            mustBe defined
          result.get.gbUser mustBe true
        }

        "should return None when name is missing" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithoutId))
            .copy(safeId = Some(exampleSafeId))
            .withPage(IndividualEmailPage, testIndividualEmail)

          val result: Option[SubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result mustBe None
        }
      }

      "for Individual with UTR journey" - {
        "should build subscription request successfully" in {
          val userAnswers = emptyUserAnswers
            .withPage(WhatIsYourNamePage, testIndividualName)
            .withPage(IndividualEmailPage, testIndividualEmail)
            .withPage(IndividualHavePhonePage, true)
            .withPage(IndividualPhoneNumberPage, testIndividualPhone)
            .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrValue))
            .copy(journeyType = Some(IndWithUtr))
            .copy(safeId = Some(exampleSafeId))

          val result: Option[SubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result mustBe defined
          val request = result.get

          request.idType                      mustBe "SAFE"
          request.idNumber                    mustBe exampleSafeId.value
          request.tradingName                 mustBe None
          request.gbUser                      mustBe true
          request.primaryContact.individual   mustBe Some(
            SubscriptionIndividualContact(testIndividualName.firstName, testIndividualName.lastName)
          )
          request.primaryContact.organisation mustBe None
          request.primaryContact.email        mustBe testIndividualEmail
          request.primaryContact.phone        mustBe Some(testIndividualPhone)
          request.secondaryContact            mustBe None
        }

        "should return None when name is missing" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithUtr))
            .copy(safeId = Some(exampleSafeId))
            .withPage(IndividualEmailPage, testIndividualEmail)
            .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrValue))

          val result: Option[SubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result mustBe None
        }

        "should populate the tradingName field correctly" in {
          val userAnswers = emptyUserAnswers
            .withPage(
              IsThisYourBusinessPage,
              IsThisYourBusinessPageDetails(
                businessDetails = BusinessDetails(
                  testTradingName,
                  AddressRegistrationResponse("Test Line 1", None, None, None, None, "GB", None),
                  safeId = testSafeId
                ),
                Some(false)
              )
            )
            .withPage(WhatIsYourNamePage, testIndividualName)
            .withPage(IndividualEmailPage, testIndividualEmail)
            .withPage(IndividualHavePhonePage, false)
            .copy(journeyType = Some(IndWithUtr))
            .copy(safeId = Some(exampleSafeId))

          val result: Option[SubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result mustBe defined
          val request = result.get

          request.tradingName mustBe Some(testTradingName)
        }
      }

      "for Organisation with UTR journey" - {
        "should build subscription request with primary contact only" in {
          val userAnswers = emptyUserAnswers
            .withPage(FirstContactNamePage, testOrganisationFirstContactName)
            .withPage(FirstContactEmailPage, testOrganisationFirstEmail)
            .withPage(FirstContactPhonePage, true)
            .withPage(FirstContactPhoneNumberPage, testOrganisationFirstPhone)
            .withPage(WhatIsTheNameOfYourBusinessPage, testBusinessName)
            .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrValue))
            .withPage(OrganisationHaveSecondContactPage, false)
            .copy(journeyType = Some(OrgWithUtr))
            .copy(safeId = Some(exampleSafeId))

          val result: Option[SubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result mustBe defined
          val request = result.get

          request.idType                      mustBe "SAFE"
          request.idNumber                    mustBe exampleSafeId.value
          request.tradingName                 mustBe Some(testBusinessName)
          request.gbUser                      mustBe true
          request.primaryContact.individual   mustBe None
          request.primaryContact.organisation mustBe Some(
            SubscriptionOrganisationContact(testOrganisationFirstContactName)
          )
          request.primaryContact.email        mustBe testOrganisationFirstEmail
          request.primaryContact.phone        mustBe Some(testOrganisationFirstPhone)
          request.secondaryContact            mustBe None
        }

        "should build subscription request with primary and secondary contact" in {
          val userAnswers = emptyUserAnswers
            .withPage(FirstContactNamePage, testOrganisationFirstContactName)
            .withPage(FirstContactEmailPage, testOrganisationFirstEmail)
            .withPage(FirstContactPhonePage, true)
            .withPage(FirstContactPhoneNumberPage, testOrganisationFirstPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, testOrganisationSecondContactName)
            .withPage(OrganisationSecondContactEmailPage, testOrganisationSecondEmail)
            .withPage(OrganisationSecondContactHavePhonePage, true)
            .withPage(OrganisationSecondContactPhoneNumberPage, testOrganisationSecondPhone)
            .withPage(WhatIsTheNameOfYourBusinessPage, testBusinessName)
            .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrValue))
            .copy(journeyType = Some(OrgWithUtr))
            .copy(safeId = Some(exampleSafeId))

          val result: Option[SubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result mustBe defined
          val request = result.get

          request.idType                            mustBe "SAFE"
          request.idNumber                          mustBe exampleSafeId.value
          request.tradingName                       mustBe Some(testBusinessName)
          request.gbUser                            mustBe true
          request.primaryContact.individual         mustBe None
          request.primaryContact.organisation       mustBe Some(
            SubscriptionOrganisationContact(testOrganisationFirstContactName)
          )
          request.primaryContact.email              mustBe testOrganisationFirstEmail
          request.primaryContact.phone              mustBe Some(testOrganisationFirstPhone)
          request.secondaryContact                  mustBe defined
          request.secondaryContact.get.individual   mustBe None
          request.secondaryContact.get.organisation mustBe Some(
            SubscriptionOrganisationContact(testOrganisationSecondContactName)
          )
          request.secondaryContact.get.email        mustBe testOrganisationSecondEmail
          request.secondaryContact.get.phone        mustBe Some(testOrganisationSecondPhone)
        }

        "should not include secondary contact when hasSecondContact is false" in {
          val userAnswers: UserAnswers = emptyUserAnswers
            .withPage(FirstContactNamePage, testOrganisationFirstContactName)
            .withPage(FirstContactEmailPage, testOrganisationFirstEmail)
            .withPage(FirstContactPhonePage, false)
            .withPage(OrganisationHaveSecondContactPage, false)
            .withPage(WhatIsTheNameOfYourBusinessPage, testBusinessName)
            .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrValue))
            .copy(journeyType = Some(OrgWithUtr))
            .copy(safeId = Some(exampleSafeId))

          val result = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result                      mustBe defined
          result.get.secondaryContact mustBe None
        }

        "should return None when primary contact name is missing" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(OrgWithUtr))
            .copy(safeId = Some(exampleSafeId))
            .withPage(FirstContactEmailPage, testOrganisationFirstEmail)
            .withPage(WhatIsTheNameOfYourBusinessPage, testBusinessName)
            .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrValue))

          val result: Option[SubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result mustBe None
        }

        "should return None when primary contact email is missing" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(OrgWithUtr))
            .copy(safeId = Some(exampleSafeId))
            .withPage(FirstContactNamePage, testOrganisationFirstContactName)
            .withPage(WhatIsTheNameOfYourBusinessPage, testBusinessName)
            .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrValue))

          val result: Option[SubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result mustBe None
        }

        "should return None when secondary contact is required but name is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(FirstContactNamePage, testOrganisationFirstContactName)
            .withPage(FirstContactEmailPage, testOrganisationFirstEmail)
            .withPage(FirstContactPhonePage, false)
            .withPage(OrganisationSecondContactHavePhonePage, true)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactEmailPage, testOrganisationSecondEmail)
            .withPage(WhatIsTheNameOfYourBusinessPage, testBusinessName)
            .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrValue))
            .copy(journeyType = Some(OrgWithUtr))
            .copy(safeId = Some(exampleSafeId))

          val result: Option[SubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result                      mustBe defined
          result.get.secondaryContact mustBe None
        }

        "should not include secondary contact when secondary contact email is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(FirstContactNamePage, testOrganisationFirstContactName)
            .withPage(FirstContactEmailPage, testOrganisationFirstEmail)
            .withPage(FirstContactPhonePage, false)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactHavePhonePage, false)
            .withPage(OrganisationSecondContactNamePage, testOrganisationSecondContactName)
            .withPage(WhatIsTheNameOfYourBusinessPage, testBusinessName)
            .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrValue))
            .copy(journeyType = Some(OrgWithUtr))
            .copy(safeId = Some(exampleSafeId))

          val result: Option[SubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result                      mustBe defined
          result.get.secondaryContact mustBe None
        }
      }

      "for Organisation without ID journey" - {
        "should build subscription request with trading name" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(OrgWithoutId))
            .withPage(RegisteredAddressInUkPage, true)
            .withPage(FirstContactNamePage, testOrganisationFirstContactName)
            .withPage(FirstContactEmailPage, testOrganisationFirstEmail)
            .withPage(FirstContactPhonePage, true)
            .withPage(FirstContactPhoneNumberPage, testOrganisationFirstPhone)
            .withPage(HaveTradingNamePage, true)
            .withPage(TradingNamePage, testTradingName)
            .withPage(OrganisationHaveSecondContactPage, false)
            .copy(safeId = Some(exampleSafeId))

          val result: Option[SubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result mustBe defined
          val request = result.get

          request.idType                      mustBe "SAFE"
          request.idNumber                    mustBe exampleSafeId.value
          request.tradingName                 mustBe Some(testTradingName)
          request.gbUser                      mustBe true
          request.primaryContact.individual   mustBe None
          request.primaryContact.organisation mustBe Some(
            SubscriptionOrganisationContact(testOrganisationFirstContactName)
          )
          request.primaryContact.email        mustBe testOrganisationFirstEmail
          request.primaryContact.phone        mustBe Some(testOrganisationFirstPhone)
          request.secondaryContact            mustBe None
        }

        "should build subscription request with secondary contact" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(OrgWithoutId))
            .withPage(RegisteredAddressInUkPage, true)
            .withPage(FirstContactNamePage, testOrganisationFirstContactName)
            .withPage(FirstContactEmailPage, testOrganisationFirstEmail)
            .withPage(FirstContactPhonePage, false)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, testOrganisationSecondContactName)
            .withPage(OrganisationSecondContactHavePhonePage, false)
            .withPage(OrganisationSecondContactEmailPage, testOrganisationSecondEmail)
            .withPage(TradingNamePage, testTradingName)
            .copy(safeId = Some(exampleSafeId))

          val result: Option[SubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result mustBe defined
          val request = result.get
          request.secondaryContact                  mustBe defined
          request.secondaryContact.get.organisation mustBe Some(
            SubscriptionOrganisationContact(testOrganisationSecondContactName)
          )
          request.secondaryContact.get.email        mustBe testOrganisationSecondEmail
        }

        "should return None when primary contact name is missing" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(OrgWithoutId))
            .copy(safeId = Some(exampleSafeId))
            .withPage(FirstContactEmailPage, testOrganisationFirstEmail)
            .withPage(TradingNamePage, testTradingName)

          val result: Option[SubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result mustBe None
        }

        "should fall back to business name when trading name is not set" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(OrgWithoutId))
            .withPage(RegisteredAddressInUkPage, true)
            .withPage(FirstContactNamePage, testOrganisationFirstContactName)
            .withPage(FirstContactEmailPage, testOrganisationFirstEmail)
            .withPage(FirstContactPhonePage, false)
            .withPage(OrganisationHaveSecondContactPage, false)
            .withPage(OrgWithoutIdBusinessNamePage, testBusinessName)
            .copy(safeId = Some(exampleSafeId))

          val result: Option[SubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result                 mustBe defined
          result.get.tradingName mustBe Some(testBusinessName)
        }

        "should use tradingName over businessName when both are set" in {
          val userAnswers = emptyUserAnswers
            .withPage(FirstContactNamePage, testOrganisationFirstContactName)
            .withPage(FirstContactEmailPage, testOrganisationFirstEmail)
            .withPage(FirstContactPhonePage, false)
            .withPage(OrganisationHaveSecondContactPage, false)
            .withPage(RegisteredAddressInUkPage, true)
            .withPage(WhatIsTheNameOfYourBusinessPage, testBusinessName)
            .withPage(HaveTradingNamePage, true)
            .withPage(TradingNamePage, testTradingName)
            .copy(journeyType = Some(OrgWithoutId))
            .copy(safeId = Some(exampleSafeId))

          val result = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result                 mustBe defined
          result.get.tradingName mustBe Some(testTradingName)
        }

      }

      "for unknown or missing journey type" - {
        "should return None" in {
          val userAnswers = emptyUserAnswers
            .copy(safeId = Some(exampleSafeId))
            .withPage(FirstContactNamePage, testOrganisationFirstContactName)
            .withPage(FirstContactEmailPage, testOrganisationFirstEmail)

          val result: Option[SubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

          result mustBe None
        }
      }

      "should populate the tradingName field correctly from an auto-matched business" in {
        val testUtr = UniqueTaxpayerReference("1234567890")

        val userAnswers = emptyUserAnswers
          .withPage(
            IsThisYourBusinessPage,
            IsThisYourBusinessPageDetails(
              businessDetails = BusinessDetails(
                testTradingName,
                AddressRegistrationResponse("Test Line 1", None, None, None, None, "GB", None),
                safeId = testSafeId
              ),
              Some(false)
            )
          )
          .withPage(UniqueTaxpayerReferenceInUserAnswers, testUtr)
          .withPage(FirstContactNamePage, testOrganisationFirstContactName)
          .withPage(FirstContactEmailPage, testOrganisationFirstEmail)
          .withPage(FirstContactPhonePage, false)
          .withPage(OrganisationHaveSecondContactPage, false)
          .copy(journeyType = Some(OrgWithUtr))
          .copy(safeId = Some(exampleSafeId))
          .copy(isCtAutoMatched = true)

        val result = subscriptionHelper.buildSubscriptionRequest(userAnswers)

        result                 mustBe defined
        result.get.tradingName mustBe Some(testTradingName)
        result.get.idNumber    mustBe exampleSafeId.value
      }
    }

  }
}
