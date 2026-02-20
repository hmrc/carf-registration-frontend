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

package services

import base.SpecBase
import cats.data.EitherT
import connectors.SubscriptionConnector
import models.*
import models.JourneyType.*
import models.countries.Country
import models.error.ApiError.{InternalServerError, NotFoundError}
import models.requests.{ContactInformation, CreateSubscriptionRequest, IndividualDetails as RequestIndividualDetails, OrganisationDetails}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, verify, when}
import pages.*
import pages.individual.{IndividualEmailPage, IndividualPhoneNumberPage, NiNumberPage, WhatIsYourNameIndividualPage}
import pages.individualWithoutId.{IndFindAddressPage, IndWithoutIdAddressPage, IndWithoutNinoNamePage}
import pages.orgWithoutId.TradingNamePage
import pages.organisation.*

import scala.concurrent.Future

class SubscriptionServiceSpec extends SpecBase {
  val mockConnector: SubscriptionConnector = mock[SubscriptionConnector]
  val testService                          = new SubscriptionService(mockConnector)

  val testSubscriptionId: SubscriptionId = SubscriptionId("XACARF1234567890")
  val testSafeId                         = "safeid"

  val testIndividualName: Name          = Name("John", "Doe")
  val testIndividualEmail               = "john.doe@example.com"
  val testIndividualPhone               = "01234567890"
  val testOrganisationFirstContactName  = "Jane Smith"
  val testOrganisationFirstEmail        = "jane.smith@example.com"
  val testOrganisationFirstPhone        = "09876543210"
  val testOrganisationSecondContactName = "joe Bloggs"
  val testOrganisationSecondEmail       = "bob.johnson@example.com"
  val testOrganisationSecondPhone       = "01122334455"
  val testTradingName                   = "Test Trading Ltd"
  val testBusinessName                  = "Test Business Ltd"
  val testNino                          = "AA123456C"
  val testUtrValue                      = "1234567890"

  val testIndWithoutIdAddress: AddressUK = AddressUK(
    addressLine1 = "123 Test Street",
    addressLine2 = Some("Testington"),
    townOrCity = "Townshire",
    county = None,
    postCode = "12345",
    countryCode = "US"
  )

  val testIndWithoutIdAddressGb: AddressUK = testIndWithoutIdAddress.copy(countryCode = "GB")

  val testIndFindAddress: IndFindAddress = IndFindAddress("SW1A 1AA", Some("10"))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
  }

  "SubscriptionService" - {
    "subscribe" - {

      "for Individual with NINO journey" - {
        "should create subscription successfully" in {
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

          val expectedRequest = CreateSubscriptionRequest(
            idType = "SAFE",
            idNumber = testSafeId,
            tradingName = None,
            gbUser = true,
            primaryContact = ContactInformation(
              individual = Some(RequestIndividualDetails(testIndividualName.firstName, testIndividualName.lastName)),
              organisation = None,
              email = testIndividualEmail,
              phone = Some(testIndividualPhone)
            ),
            secondaryContact = None
          )

          when(mockConnector.createSubscription(eqTo(expectedRequest))(any(), any()))
            .thenReturn(EitherT.rightT[Future, models.error.ApiError](testSubscriptionId))

          val result = testService.subscribe(userAnswers).futureValue

          result mustBe Right(testSubscriptionId)
          verify(mockConnector).createSubscription(eqTo(expectedRequest))(any(), any())
        }

        "should return error when connector fails" in {
          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithNino))
            .set(WhatIsYourNameIndividualPage, testIndividualName)
            .success
            .value
            .set(IndividualEmailPage, testIndividualEmail)
            .success
            .value

          when(mockConnector.createSubscription(any())(any(), any()))
            .thenReturn(EitherT.leftT[Future, SubscriptionId](InternalServerError))

          val result = testService.subscribe(userAnswers).futureValue

          result mustBe Left(InternalServerError)
        }
      }

      "for Individual without ID journey" - {
        "should create subscription successfully" in {
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
            .set(IndWithoutIdAddressPage, testIndWithoutIdAddress)
            .success
            .value

          val expectedRequest = CreateSubscriptionRequest(
            idType = "SAFE",
            idNumber = testSafeId,
            tradingName = None,
            gbUser = false,
            primaryContact = ContactInformation(
              individual = Some(RequestIndividualDetails(testIndividualName.firstName, testIndividualName.lastName)),
              organisation = None,
              email = testIndividualEmail,
              phone = Some(testIndividualPhone)
            ),
            secondaryContact = None
          )

          when(mockConnector.createSubscription(eqTo(expectedRequest))(any(), any()))
            .thenReturn(EitherT.rightT[Future, models.error.ApiError](testSubscriptionId))

          val result = testService.subscribe(userAnswers).futureValue

          result mustBe Right(testSubscriptionId)
        }

        "for Individual with UTR journey" - {
          "should create subscription successfully" in {
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

            val expectedRequest = CreateSubscriptionRequest(
              idType = "SAFE",
              idNumber = testSafeId,
              tradingName = None,
              gbUser = true,
              primaryContact = ContactInformation(
                individual = Some(RequestIndividualDetails(testIndividualName.firstName, testIndividualName.lastName)),
                organisation = None,
                email = testIndividualEmail,
                phone = Some(testIndividualPhone)
              ),
              secondaryContact = None
            )

            when(mockConnector.createSubscription(eqTo(expectedRequest))(any(), any()))
              .thenReturn(EitherT.rightT[Future, models.error.ApiError](testSubscriptionId))

            val result = testService.subscribe(userAnswers).futureValue

            result mustBe Right(testSubscriptionId)
          }
        }
      }

      "for Organisation with UTR journey" - {
        "should create subscription with primary contact only" in {
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

          val expectedRequest = CreateSubscriptionRequest(
            idType = "SAFE",
            idNumber = testSafeId,
            tradingName = Some(testBusinessName),
            gbUser = true,
            primaryContact = ContactInformation(
              individual = None,
              organisation = Some(OrganisationDetails(testOrganisationFirstContactName)),
              email = testOrganisationFirstEmail,
              phone = Some(testOrganisationFirstPhone)
            ),
            secondaryContact = None
          )

          when(mockConnector.createSubscription(eqTo(expectedRequest))(any(), any()))
            .thenReturn(EitherT.rightT[Future, models.error.ApiError](testSubscriptionId))

          val result = testService.subscribe(userAnswers).futureValue

          result mustBe Right(testSubscriptionId)
          verify(mockConnector).createSubscription(eqTo(expectedRequest))(any(), any())
        }

        "should create subscription with primary and secondary contact" in {
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

          val expectedRequest = CreateSubscriptionRequest(
            idType = "SAFE",
            idNumber = testSafeId,
            tradingName = Some(testBusinessName),
            gbUser = true,
            primaryContact = ContactInformation(
              individual = None,
              organisation = Some(OrganisationDetails(testOrganisationFirstContactName)),
              email = testOrganisationFirstEmail,
              phone = Some(testOrganisationFirstPhone)
            ),
            secondaryContact = Some(
              ContactInformation(
                individual = None,
                organisation = Some(OrganisationDetails(testOrganisationSecondContactName)),
                email = testOrganisationSecondEmail,
                phone = Some(testOrganisationSecondPhone)
              )
            )
          )

          when(mockConnector.createSubscription(eqTo(expectedRequest))(any(), any()))
            .thenReturn(EitherT.rightT[Future, models.error.ApiError](testSubscriptionId))

          val result = testService.subscribe(userAnswers).futureValue

          result mustBe Right(testSubscriptionId)
          verify(mockConnector).createSubscription(eqTo(expectedRequest))(any(), any())
        }
      }

      "for Organisation without ID journey" - {
        "should create subscription with trading name" in {
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

          val expectedRequest = CreateSubscriptionRequest(
            idType = "SAFE",
            idNumber = testSafeId,
            tradingName = Some(testTradingName),
            gbUser = true,
            primaryContact = ContactInformation(
              individual = None,
              organisation = Some(OrganisationDetails(testOrganisationFirstContactName)),
              email = testOrganisationFirstEmail,
              phone = Some(testOrganisationFirstPhone)
            ),
            secondaryContact = None
          )

          when(mockConnector.createSubscription(eqTo(expectedRequest))(any(), any()))
            .thenReturn(EitherT.rightT[Future, models.error.ApiError](testSubscriptionId))

          val result = testService.subscribe(userAnswers).futureValue

          result mustBe Right(testSubscriptionId)
          verify(mockConnector).createSubscription(eqTo(expectedRequest))(any(), any())
        }

      }
    }
  }
}
