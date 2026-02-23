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
import base.SpecBase
import models.*
import models.JourneyType.*
import models.requests.{ContactInformation, CreateSubscriptionRequest, IndividualDetails, OrganisationDetails}
import pages.*
import pages.individual.{IndividualEmailPage, IndividualPhoneNumberPage, NiNumberPage, WhatIsYourNameIndividualPage}
import pages.individualWithoutId.{IndFindAddressPage, IndWithoutIdAddressPagePrePop, IndWithoutNinoNamePage}
import pages.orgWithoutId.TradingNamePage
import pages.organisation.*

class SubscriptionHelperSpec extends SpecBase {

  val testHelper = new SubscriptionHelper()

  val testSafeId                        = "SAFEID"
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

  val testIndWithoutIdAddress: AddressUK = AddressUK(
    addressLine1 = "123 Test Street",
    addressLine2 = Some("Testington"),
    townOrCity = "Townshire",
    county = None,
    postCode = "12345",
    countryCode = "UK"
  )

  val testIndWithoutIdAddressGb: AddressUK = testIndWithoutIdAddress.copy(countryCode = "GB")
  val testIndFindAddress: IndFindAddress   = IndFindAddress("SW1A 1AA", Some("10"))

  "SubscriptionHelper" - {

    "buildSubscriptionRequest" - {

      "for Individual with NINO journey" - {
        "should build subscription request successfully with all required fields" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithNino))
            .set(WhatIsYourNameIndividualPage, testIndividualName)
            .success
            .value
            .set(IndividualEmailPage, testIndividualEmail)
            .success
            .value
            .set(IndividualPhoneNumberPage, testIndividualPhone)
            .success
            .value
            .set(NiNumberPage, testNino)
            .success
            .value

          val result = testHelper.buildSubscriptionRequest(userAnswers)

          result mustBe defined
          val request = result.get

          request.idType                      mustBe "SAFE"
          request.idNumber                    mustBe testSafeId
          request.tradingName                 mustBe None
          request.gbUser                      mustBe true
          request.primaryContact.individual   mustBe Some(
            RequestIndividualDetails(testIndividualName.firstName, testIndividualName.lastName)
          )
          request.primaryContact.organisation mustBe None
          request.primaryContact.email        mustBe testIndividualEmail
          request.primaryContact.phone        mustBe Some(testIndividualPhone)
          request.secondaryContact            mustBe None
        }

        "should build subscription request without optional phone number" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithNino))
            .set(WhatIsYourNameIndividualPage, testIndividualName)
            .success
            .value
            .set(IndividualEmailPage, testIndividualEmail)
            .success
            .value
            .set(NiNumberPage, testNino)
            .success
            .value

          val result: Option[CreateSubscriptionRequest] = testHelper.buildSubscriptionRequest(userAnswers)

          result mustBe defined
          request.primaryContact.phone mustBe None
        }

        "should return None when name is missing" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithNino))
            .set(IndividualEmailPage, testIndividualEmail)
            .success
            .value
            .set(NiNumberPage, testNino)
            .success
            .value

          val result = testHelper.buildSubscriptionRequest(userAnswers)

          result mustBe None
        }

        "should return None when email is missing" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithNino))
            .set(WhatIsYourNameIndividualPage, testIndividualName)
            .success
            .value
            .set(NiNumberPage, testNino)
            .success
            .value

          val result = testHelper.buildSubscriptionRequest(userAnswers)

          result mustBe None
        }
      }

      "for Individual without ID journey" - {
        "should build subscription request successfully" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithoutId))
            .set(IndWithoutNinoNamePage, testIndividualName)
            .success
            .value
            .set(IndividualEmailPage, testIndividualEmail)
            .success
            .value
            .set(IndividualPhoneNumberPage, testIndividualPhone)
            .success
            .value
            .set(IndWithoutIdAddressPagePrePop, testIndWithoutIdAddress)
            .success
            .value

          val result = testHelper.buildSubscriptionRequest(userAnswers)

          result mustBe defined
          val request = result.get

          request.idType                      mustBe "SAFE"
          request.idNumber                    mustBe testSafeId
          request.tradingName                 mustBe None
          request.gbUser                      mustBe false
          request.primaryContact.individual   mustBe Some(
            RequestIndividualDetails(testIndividualName.firstName, testIndividualName.lastName)
          )
          request.primaryContact.organisation mustBe None
          request.primaryContact.email        mustBe testIndividualEmail
          request.primaryContact.phone        mustBe Some(testIndividualPhone)
          request.secondaryContact            mustBe None
        }

        "should identify GB user when address is GB" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithoutId))
            .set(IndWithoutNinoNamePage, testIndividualName)
            .success
            .value
            .set(IndividualEmailPage, testIndividualEmail)
            .success
            .value
            .set(IndWithoutIdAddressPagePrePop, testIndWithoutIdAddressGb)
            .success
            .value

          val result = testHelper.buildSubscriptionRequest(userAnswers)

          result            mustBe defined
          result.get.gbUser mustBe true
        }

        "should identify GB user when address lookup is present" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithoutId))
            .set(IndWithoutNinoNamePage, testIndividualName)
            .success
            .value
            .set(IndividualEmailPage, testIndividualEmail)
            .success
            .value
            .set(IndFindAddressPage, testIndFindAddress)
            .success
            .value

          val result = testHelper.buildSubscriptionRequest(userAnswers)

          result            mustBe defined
          result.get.gbUser mustBe true
        }

        "should return None when name is missing" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithoutId))
            .set(IndividualEmailPage, testIndividualEmail)
            .success
            .value

          val result = testHelper.buildSubscriptionRequest(userAnswers)

          result mustBe None
        }
      }

      "for Individual with UTR journey" - {
        "should build subscription request successfully" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithUtr))
            .set(WhatIsYourNamePage, testIndividualName)
            .success
            .value
            .set(IndividualEmailPage, testIndividualEmail)
            .success
            .value
            .set(IndividualPhoneNumberPage, testIndividualPhone)
            .success
            .value
            .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrValue))
            .success
            .value

          val result = testHelper.buildSubscriptionRequest(userAnswers)

          result mustBe defined
          val request = result.get

          request.idType                      mustBe "SAFE"
          request.idNumber                    mustBe testSafeId
          request.tradingName                 mustBe None
          request.gbUser                      mustBe true
          request.primaryContact.individual   mustBe Some(
            RequestIndividualDetails(testIndividualName.firstName, testIndividualName.lastName)
          )
          request.primaryContact.organisation mustBe None
          request.primaryContact.email        mustBe testIndividualEmail
          request.primaryContact.phone        mustBe Some(testIndividualPhone)
          request.secondaryContact            mustBe None
        }

        "should return None when name is missing" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithUtr))
            .set(IndividualEmailPage, testIndividualEmail)
            .success
            .value
            .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrValue))
            .success
            .value

          val result = testHelper.buildSubscriptionRequest(userAnswers)

          result mustBe None
        }
      }

      "for Organisation with UTR journey" - {
        "should build subscription request with primary contact only" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(OrgWithUtr))
            .set(FirstContactNamePage, testOrganisationFirstContactName)
            .success
            .value
            .set(FirstContactEmailPage, testOrganisationFirstEmail)
            .success
            .value
            .set(FirstContactPhoneNumberPage, testOrganisationFirstPhone)
            .success
            .value
            .set(WhatIsTheNameOfYourBusinessPage, testBusinessName)
            .success
            .value
            .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrValue))
            .success
            .value
            .set(OrganisationHaveSecondContactPage, false)
            .success
            .value

          val result = testHelper.buildSubscriptionRequest(userAnswers)

          result mustBe defined
          val request = result.get

          request.idType                      mustBe "SAFE"
          request.idNumber                    mustBe testSafeId
          request.tradingName                 mustBe Some(testBusinessName)
          request.gbUser                      mustBe true
          request.primaryContact.individual   mustBe None
          request.primaryContact.organisation mustBe Some(OrganisationDetails(testOrganisationFirstContactName))
          request.primaryContact.email        mustBe testOrganisationFirstEmail
          request.primaryContact.phone        mustBe Some(testOrganisationFirstPhone)
          request.secondaryContact            mustBe None
        }

        "should build subscription request with primary and secondary contact" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(OrgWithUtr))
            .set(FirstContactNamePage, testOrganisationFirstContactName)
            .success
            .value
            .set(FirstContactEmailPage, testOrganisationFirstEmail)
            .success
            .value
            .set(FirstContactPhoneNumberPage, testOrganisationFirstPhone)
            .success
            .value
            .set(OrganisationHaveSecondContactPage, true)
            .success
            .value
            .set(OrganisationSecondContactNamePage, testOrganisationSecondContactName)
            .success
            .value
            .set(OrganisationSecondContactEmailPage, testOrganisationSecondEmail)
            .success
            .value
            .set(OrganisationSecondContactPhoneNumberPage, testOrganisationSecondPhone)
            .success
            .value
            .set(WhatIsTheNameOfYourBusinessPage, testBusinessName)
            .success
            .value
            .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrValue))
            .success
            .value

          val result = testHelper.buildSubscriptionRequest(userAnswers)

          result mustBe defined
          val request = result.get

          request.idType                            mustBe "SAFE"
          request.idNumber                          mustBe testSafeId
          request.tradingName                       mustBe Some(testBusinessName)
          request.gbUser                            mustBe true
          request.primaryContact.individual         mustBe None
          request.primaryContact.organisation       mustBe Some(OrganisationDetails(testOrganisationFirstContactName))
          request.primaryContact.email              mustBe testOrganisationFirstEmail
          request.primaryContact.phone              mustBe Some(testOrganisationFirstPhone)
          request.secondaryContact                  mustBe defined
          request.secondaryContact.get.individual   mustBe None
          request.secondaryContact.get.organisation mustBe Some(OrganisationDetails(testOrganisationSecondContactName))
          request.secondaryContact.get.email        mustBe testOrganisationSecondEmail
          request.secondaryContact.get.phone        mustBe Some(testOrganisationSecondPhone)
        }

        "should not include secondary contact when hasSecondContact is false" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(OrgWithUtr))
            .set(FirstContactNamePage, testOrganisationFirstContactName)
            .success
            .value
            .set(FirstContactEmailPage, testOrganisationFirstEmail)
            .success
            .value
            .set(OrganisationHaveSecondContactPage, false)
            .success
            .value
            .set(WhatIsTheNameOfYourBusinessPage, testBusinessName)
            .success
            .value
            .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrValue))
            .success
            .value

          val result = testHelper.buildSubscriptionRequest(userAnswers)

          result                      mustBe defined
          result.get.secondaryContact mustBe None
        }

        "should return None when primary contact name is missing" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(OrgWithUtr))
            .set(FirstContactEmailPage, testOrganisationFirstEmail)
            .success
            .value
            .set(WhatIsTheNameOfYourBusinessPage, testBusinessName)
            .success
            .value
            .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrValue))
            .success
            .value

          val result = testHelper.buildSubscriptionRequest(userAnswers)

          result mustBe None
        }

        "should return None when primary contact email is missing" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(OrgWithUtr))
            .set(FirstContactNamePage, testOrganisationFirstContactName)
            .success
            .value
            .set(WhatIsTheNameOfYourBusinessPage, testBusinessName)
            .success
            .value
            .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrValue))
            .success
            .value

          val result = testHelper.buildSubscriptionRequest(userAnswers)

          result mustBe None
        }

        "should return None when secondary contact is required but name is missing" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(OrgWithUtr))
            .set(FirstContactNamePage, testOrganisationFirstContactName)
            .success
            .value
            .set(FirstContactEmailPage, testOrganisationFirstEmail)
            .success
            .value
            .set(OrganisationHaveSecondContactPage, true)
            .success
            .value
            .set(OrganisationSecondContactEmailPage, testOrganisationSecondEmail)
            .success
            .value
            .set(WhatIsTheNameOfYourBusinessPage, testBusinessName)
            .success
            .value
            .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrValue))
            .success
            .value

          val result = testHelper.buildSubscriptionRequest(userAnswers)

          result mustBe None
        }

        "should return None when secondary contact is required but email is missing" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(OrgWithUtr))
            .set(FirstContactNamePage, testOrganisationFirstContactName)
            .success
            .value
            .set(FirstContactEmailPage, testOrganisationFirstEmail)
            .success
            .value
            .set(OrganisationHaveSecondContactPage, true)
            .success
            .value
            .set(OrganisationSecondContactNamePage, testOrganisationSecondContactName)
            .success
            .value
            .set(WhatIsTheNameOfYourBusinessPage, testBusinessName)
            .success
            .value
            .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrValue))
            .success
            .value

          val result = testHelper.buildSubscriptionRequest(userAnswers)

          result mustBe None
        }
      }

      "for Organisation without ID journey" - {
        "should build subscription request with trading name" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(OrgWithoutId))
            .set(FirstContactNamePage, testOrganisationFirstContactName)
            .success
            .value
            .set(FirstContactEmailPage, testOrganisationFirstEmail)
            .success
            .value
            .set(FirstContactPhoneNumberPage, testOrganisationFirstPhone)
            .success
            .value
            .set(TradingNamePage, testTradingName)
            .success
            .value
            .set(OrganisationHaveSecondContactPage, false)
            .success
            .value
            .set(RegisteredAddressInUkPage, true)
            .success
            .value

          val result = testHelper.buildSubscriptionRequest(userAnswers)

          result mustBe defined
          val request = result.get

          request.idType                      mustBe "SAFE"
          request.idNumber                    mustBe testSafeId
          request.tradingName                 mustBe Some(testTradingName)
          request.gbUser                      mustBe true
          request.primaryContact.individual   mustBe None
          request.primaryContact.organisation mustBe Some(OrganisationDetails(testOrganisationFirstContactName))
          request.primaryContact.email        mustBe testOrganisationFirstEmail
          request.primaryContact.phone        mustBe Some(testOrganisationFirstPhone)
          request.secondaryContact            mustBe None
        }

        "should build subscription request with secondary contact" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(OrgWithoutId))
            .set(FirstContactNamePage, testOrganisationFirstContactName)
            .success
            .value
            .set(FirstContactEmailPage, testOrganisationFirstEmail)
            .success
            .value
            .set(OrganisationHaveSecondContactPage, true)
            .success
            .value
            .set(OrganisationSecondContactNamePage, testOrganisationSecondContactName)
            .success
            .value
            .set(OrganisationSecondContactEmailPage, testOrganisationSecondEmail)
            .success
            .value
            .set(TradingNamePage, testTradingName)
            .success
            .value
            .set(RegisteredAddressInUkPage, true)
            .success
            .value

          val result = testHelper.buildSubscriptionRequest(userAnswers)

          result mustBe defined
          val request = result.get
          request.secondaryContact                  mustBe defined
          request.secondaryContact.get.organisation mustBe Some(OrganisationDetails(testOrganisationSecondContactName))
          request.secondaryContact.get.email        mustBe testOrganisationSecondEmail
        }

        "should return None when primary contact name is missing" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(OrgWithoutId))
            .set(FirstContactEmailPage, testOrganisationFirstEmail)
            .success
            .value
            .set(TradingNamePage, testTradingName)
            .success
            .value

          val result = testHelper.buildSubscriptionRequest(userAnswers)

          result mustBe None
        }
      }

      "for unknown or missing journey type" - {
        "should return None" in {
          val userAnswers = emptyUserAnswers
            .set(FirstContactNamePage, testOrganisationFirstContactName)
            .success
            .value
            .set(FirstContactEmailPage, testOrganisationFirstEmail)
            .success
            .value

          val result = testHelper.buildSubscriptionRequest(userAnswers)

          result mustBe None
        }
      }
    }

    "isGBUser logic" - {
      "should identify GB user when business has UTR" in {
        val userAnswers = emptyUserAnswers
          .copy(journeyType = Some(OrgWithUtr))
          .set(FirstContactNamePage, testOrganisationFirstContactName)
          .success
          .value
          .set(FirstContactEmailPage, testOrganisationFirstEmail)
          .success
          .value
          .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrValue))
          .success
          .value

        val result = testHelper.buildSubscriptionRequest(userAnswers)

        result            mustBe defined
        result.get.gbUser mustBe true
      }

      "should not identify GB user when UTR is empty" in {
        val userAnswers = emptyUserAnswers
          .copy(journeyType = Some(OrgWithUtr))
          .set(FirstContactNamePage, testOrganisationFirstContactName)
          .success
          .value
          .set(FirstContactEmailPage, testOrganisationFirstEmail)
          .success
          .value
          .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("   "))
          .success
          .value

        val result = testHelper.buildSubscriptionRequest(userAnswers)

        result            mustBe defined
        result.get.gbUser mustBe false
      }

      "should identify GB user when individual has NINO" in {
        val userAnswers = emptyUserAnswers
          .copy(journeyType = Some(IndWithNino))
          .set(WhatIsYourNameIndividualPage, testIndividualName)
          .success
          .value
          .set(IndividualEmailPage, testIndividualEmail)
          .success
          .value
          .set(NiNumberPage, testNino)
          .success
          .value

        val result = testHelper.buildSubscriptionRequest(userAnswers)

        result            mustBe defined
        result.get.gbUser mustBe true
      }

      "should not identify GB user when NINO is empty" in {
        val userAnswers = emptyUserAnswers
          .copy(journeyType = Some(IndWithNino))
          .set(WhatIsYourNameIndividualPage, testIndividualName)
          .success
          .value
          .set(IndividualEmailPage, testIndividualEmail)
          .success
          .value
          .set(NiNumberPage, "   ")
          .success
          .value

        val result = testHelper.buildSubscriptionRequest(userAnswers)

        result            mustBe defined
        result.get.gbUser mustBe false
      }

      "should identify GB user when registered address is in UK" in {
        val userAnswers = emptyUserAnswers
          .copy(journeyType = Some(OrgWithoutId))
          .set(FirstContactNamePage, testOrganisationFirstContactName)
          .success
          .value
          .set(FirstContactEmailPage, testOrganisationFirstEmail)
          .success
          .value
          .set(RegisteredAddressInUkPage, true)
          .success
          .value

        val result = testHelper.buildSubscriptionRequest(userAnswers)

        result            mustBe defined
        result.get.gbUser mustBe true
      }

      "should not identify GB user when registered address is not in UK" in {
        val userAnswers = emptyUserAnswers
          .copy(journeyType = Some(OrgWithoutId))
          .set(FirstContactNamePage, testOrganisationFirstContactName)
          .success
          .value
          .set(FirstContactEmailPage, testOrganisationFirstEmail)
          .success
          .value
          .set(RegisteredAddressInUkPage, false)
          .success
          .value

        val result = testHelper.buildSubscriptionRequest(userAnswers)

        result            mustBe defined
        result.get.gbUser mustBe false
      }

      "should not identify GB user when no GB criteria are met" in {
        val userAnswers = emptyUserAnswers
          .copy(journeyType = Some(IndWithoutId))
          .set(IndWithoutNinoNamePage, testIndividualName)
          .success
          .value
          .set(IndividualEmailPage, testIndividualEmail)
          .success
          .value
          .set(IndWithoutIdAddressPagePrePop, testIndWithoutIdAddress)
          .success
          .value

        val result = testHelper.buildSubscriptionRequest(userAnswers)

        result            mustBe defined
        result.get.gbUser mustBe false
      }
    }
  }
}
