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

package services

import base.SpecBase
import cats.data.EitherT
import connectors.RegistrationConnector
import models.error.ApiError.{InternalServerError, NotFoundError}
import models.error.{ApiError, DataError}
import models.requests.*
import models.responses.{AddressRegistrationResponse, RegisterIndividualWithIdResponse, RegisterOrganisationWithIdResponse}
import models.{BusinessDetails, IndividualDetails, Name, RegistrationType, UniqueTaxpayerReference, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalactic.Prettifier.default
import pages.organisation.{RegistrationTypePage, UniqueTaxpayerReferenceInUserAnswers, WhatIsTheNameOfYourBusinessPage, WhatIsYourNamePage}

import java.time.LocalDate
import scala.concurrent.Future

class RegistrationServiceSpec extends SpecBase {
  val mockConnector: RegistrationConnector = mock[RegistrationConnector]
  val testService                          = new RegistrationService(mockConnector)
  val testOrgType: RegistrationType        = RegistrationType.LimitedCompany
  val ninoOkFullIndividualResponse         = "JX123456D"
  val ninoNotFound                         = "XX123456D"
  val ninoInternalServerError              = "YX123456D"
  val validBirthDate: LocalDate            = LocalDate.of(2000, 1, 1)

  val testAddress = AddressRegistrationResponse(
    addressLine1 = "123 Main Street",
    addressLine2 = Some("Birmingham"),
    addressLine3 = None,
    addressLine4 = None,
    postalCode = Some("B23 2AZ"),
    countryCode = "GB"
  )

  val testRegisterIndividualWithIdSuccessResponse = RegisterIndividualWithIdResponse(
    safeId = "testSafeId",
    firstName = "Floriane",
    lastName = "Yammel",
    middleName = Some("Exie"),
    address = testAddress
  )

  val validName = Name(
    testRegisterIndividualWithIdSuccessResponse.firstName,
    testRegisterIndividualWithIdSuccessResponse.lastName
  )

  val orgUkBusinessResponse = RegisterOrganisationWithIdResponse(
    safeId = "testSafeId",
    code = Some("0000"),
    organisationName = "Agent ABC Ltd",
    address = AddressRegistrationResponse(
      addressLine1 = "2 High Street",
      addressLine2 = Some("Birmingham"),
      addressLine3 = None,
      addressLine4 = None,
      postalCode = Some("B23 2AZ"),
      countryCode = "GB"
    )
  )

  val organisationUserAnswersUtr: UserAnswers = UserAnswers(userAnswersId)
    .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrString))
    .success
    .value
    .set(WhatIsTheNameOfYourBusinessPage, orgUkBusinessResponse.organisationName)
    .success
    .value
    .set(RegistrationTypePage, testOrgType)
    .success
    .value

  val individualDetailsUtrUserAnswers: UserAnswers = UserAnswers(userAnswersId)
    .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("5234567890"))
    .success
    .value
    .set(WhatIsYourNamePage, Name("ST firstName", "ST lastName"))
    .success
    .value

  val utrNotPresentInIndividualDetailsUserAnswers: UserAnswers = UserAnswers(userAnswersId)
    .set(WhatIsYourNamePage, Name("ST firstName", "ST lastName"))
    .success
    .value

  val nameNotPresentIndividualDetailsUserAnswers: UserAnswers = UserAnswers(userAnswersId)
    .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("5234567890"))
    .success
    .value

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
  }

  "RegistrationService" - {
    "getBusinessWithUtr when the user has a ct enrolment should" - {
      "return business details when the connector finds a match" in {
        val expectedRequest = RegOrgWithIdCTAutoMatchRequest(
          requiresNameMatch = false,
          IDNumber = testUtrString,
          IDType = "UTR"
        )

        when(mockConnector.organisationWithUtrCTAutoMatch(eqTo(expectedRequest))(any()))
          .thenReturn(EitherT.rightT[Future, ApiError](orgUkBusinessResponse))

        val result = testService
          .getBusinessWithUtr(emptyUserAnswers.copy(isCtAutoMatched = true), testUtrString)
          .futureValue

        result mustBe Right(BusinessDetails(orgUkBusinessResponse.organisationName, orgUkBusinessResponse.address))
        verify(mockConnector).organisationWithUtrCTAutoMatch(eqTo(expectedRequest))(any())
      }

      "return a not found error when the connector does not find a match" in {
        when(mockConnector.organisationWithUtrCTAutoMatch(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegisterOrganisationWithIdResponse](NotFoundError))

        val result = testService
          .getBusinessWithUtr(emptyUserAnswers.copy(isCtAutoMatched = true), testUtrString)
          .futureValue

        result mustBe Left(NotFoundError)
      }
    }

    "getBusinessWithUtr when the user is on the manual entry journey" - {
      "return business details when UserAnswers is complete and connector finds a match" in {
        val expectedRequest = RegOrgWithIdNonAutoMatchRequest(
          requiresNameMatch = true,
          IDNumber = testUtrString,
          IDType = "UTR",
          organisationName = orgUkBusinessResponse.organisationName,
          organisationType = testOrgType.code
        )

        when(mockConnector.organisationWithUtrNonAutoMatch(eqTo(expectedRequest))(any()))
          .thenReturn(EitherT.rightT[Future, ApiError](orgUkBusinessResponse))

        val result = testService.getBusinessWithUtr(organisationUserAnswersUtr, testUtrString).futureValue

        result mustBe Right(BusinessDetails(orgUkBusinessResponse.organisationName, orgUkBusinessResponse.address))
        verify(mockConnector).organisationWithUtrNonAutoMatch(eqTo(expectedRequest))(any())
      }

      "return a data error when UserAnswers is missing data" in {
        val incompleteUserAnswers = UserAnswers(userAnswersId)
        val result                = testService.getBusinessWithUtr(incompleteUserAnswers, testUtrString).futureValue

        result mustBe Left(DataError)
        verify(mockConnector, never()).organisationWithUtrNonAutoMatch(any())(any())
      }

      "return an internal server error when one is returned by the connector" in {
        when(mockConnector.organisationWithUtrNonAutoMatch(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegisterOrganisationWithIdResponse](InternalServerError))
        val result =
          testService.getBusinessWithUtr(organisationUserAnswersUtr, testUtrString).futureValue

        result mustBe Left(InternalServerError)
      }
    }

    "getIndividualByNino method should" - {
      "successfully return an individual's details when the connector returns them successfully" in {
        when(mockConnector.individualWithNino(any[RegisterIndividualWithNinoRequest])(any()))
          .thenReturn(EitherT.rightT[Future, ApiError](testRegisterIndividualWithIdSuccessResponse))

        val result = testService
          .getIndividualByNino(ninoOkFullIndividualResponse, validName, validBirthDate)
          .futureValue

        result mustBe Right(
          IndividualDetails(
            safeId = "testSafeId",
            firstName = "Floriane",
            lastName = "Yammel",
            middleName = Some("Exie"),
            address = testAddress
          )
        )
      }

      "return a not found error when the connector could not get a record match for this user" in {
        when(mockConnector.individualWithNino(any[RegisterIndividualWithNinoRequest])(any()))
          .thenReturn(EitherT.leftT[Future, RegisterIndividualWithIdResponse](NotFoundError))

        val result = testService.getIndividualByNino(ninoNotFound, validName, validBirthDate).futureValue
        result mustBe Left(NotFoundError)
      }
    }

    "getIndividualByUtr method should" - {
      "return individual details when UserAnswers is complete and connector finds a match" in {
        val expectedRequest = RegisterIndividualWithUtrRequest(
          requiresNameMatch = true,
          IDNumber = "5234567890",
          IDType = "UTR",
          firstName = "ST firstName",
          lastName = "ST lastName"
        )
        when(mockConnector.individualWithUtr(eqTo(expectedRequest))(any()))
          .thenReturn(EitherT.rightT[Future, ApiError](testRegisterIndividualWithIdSuccessResponse))

        val result = testService.getIndividualByUtr(individualDetailsUtrUserAnswers).futureValue

        result mustBe Right(
          IndividualDetails(
            safeId = "testSafeId",
            firstName = "Floriane",
            lastName = "Yammel",
            middleName = Some("Exie"),
            address = testAddress
          )
        )
        verify(mockConnector).individualWithUtr(eqTo(expectedRequest))(any())
      }

      "return a not found error when the connector could not get a record match for this user" in {
        when(mockConnector.individualWithUtr(any[RegisterIndividualWithUtrRequest])(any()))
          .thenReturn(EitherT.leftT[Future, RegisterIndividualWithIdResponse](NotFoundError))

        val result = testService.getIndividualByUtr(individualDetailsUtrUserAnswers).futureValue
        result mustBe Left(NotFoundError)
      }

      "return a data error when the UserAnswers does not contain a Utr" in {
        val result = testService.getIndividualByUtr(utrNotPresentInIndividualDetailsUserAnswers).futureValue

        result mustBe Left(DataError)
        verify(mockConnector, never()).individualWithUtr(any())(any())
      }

      "return None when the UserAnswers does not contain a Name" in {
        val result = testService.getIndividualByUtr(nameNotPresentIndividualDetailsUserAnswers).futureValue

        result mustBe Left(DataError)
        verify(mockConnector, never()).individualWithUtr(any())(any())
      }

      "return an internal server error when the connector returns one" in {
        when(mockConnector.individualWithUtr(any[RegisterIndividualWithUtrRequest])(any()))
          .thenReturn(EitherT.leftT[Future, RegisterIndividualWithIdResponse](InternalServerError))

        val result = testService.getIndividualByUtr(individualDetailsUtrUserAnswers).futureValue

        result mustBe Left(InternalServerError)
      }
    }
  }
}
