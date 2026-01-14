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
import models.error.ApiError
import models.error.ApiError.{InternalServerError, NotFoundError}
import models.requests.{RegisterIndividualWithNinoRequest, RegisterIndividualWithUtrRequest, RegisterOrganisationWithIdRequest}
import models.responses.{RegisterIndividualWithIdResponse, RegisterOrganisationWithIdResponse}
import models.{Address, BusinessDetails, IndividualDetails, Name, OrganisationRegistrationType, UniqueTaxpayerReference, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, reset, verify, when}
import pages.organisation.{OrganisationRegistrationTypePage, UniqueTaxpayerReferenceInUserAnswers, WhatIsTheNameOfYourBusinessPage, WhatIsYourNamePage}

import java.time.LocalDate
import scala.concurrent.Future

class RegistrationServiceSpec extends SpecBase {
  val mockConnector: RegistrationConnector = mock[RegistrationConnector]
  val testService                          = new RegistrationService(mockConnector)
  val testOrgType                          = OrganisationRegistrationType.LimitedCompany
  val ninoOkFullIndividualResponse         = "JX123456D"
  val ninoNotFound                         = "XX123456D"
  val ninoInternalServerError              = "YX123456D"
  val validBirthDate: LocalDate            = LocalDate.of(2000, 1, 1)

  val testAddress = Address(
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
    address = Address(
      addressLine1 = "2 High Street",
      addressLine2 = Some("Birmingham"),
      addressLine3 = None,
      addressLine4 = None,
      postalCode = Some("B23 2AZ"),
      countryCode = "GB"
    )
  )

  val organisationUserAnswersUtr = UserAnswers(userAnswersId)
    .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtr.uniqueTaxPayerReference))
    .success
    .value
    .set(WhatIsTheNameOfYourBusinessPage, orgUkBusinessResponse.organisationName)
    .success
    .value
    .set(OrganisationRegistrationTypePage, testOrgType)
    .success
    .value

  val individualDetailsUtrUserAnswers = UserAnswers(userAnswersId)
    .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("5234567890"))
    .success
    .value
    .set(WhatIsYourNamePage, Name("ST firstName", "ST lastName"))
    .success
    .value

  val utrNotPresentInIndividualDetailsUserAnswers = UserAnswers(userAnswersId)
    .set(WhatIsYourNamePage, Name("ST firstName", "ST lastName"))
    .success
    .value

  val nameNotPresentIndividualDetailsUserAnswers = UserAnswers(userAnswersId)
    .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("5234567890"))
    .success
    .value

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
  }

  "RegistrationService" - {
    "getBusinessWithEnrolmentCtUtr should" - {
      "return business details when the connector finds a match" in {
        val expectedRequest = RegisterOrganisationWithIdRequest(
          requiresNameMatch = false,
          IDNumber = testUtr.uniqueTaxPayerReference,
          IDType = "UTR",
          organisationName = None,
          organisationType = None
        )
        when(mockConnector.organisationWithUtr(eqTo(expectedRequest))(any()))
          .thenReturn(EitherT.rightT[Future, ApiError](orgUkBusinessResponse))
        val result          = testService.getBusinessWithEnrolmentCtUtr(testUtr.uniqueTaxPayerReference)
        val business        = result.futureValue
        business mustBe Some(BusinessDetails(orgUkBusinessResponse.organisationName, orgUkBusinessResponse.address))
        verify(mockConnector).organisationWithUtr(eqTo(expectedRequest))(any())
      }

      "return None when the connector does not find a match" in {
        when(mockConnector.organisationWithUtr(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegisterOrganisationWithIdResponse](NotFoundError))
        val result   = testService.getBusinessWithEnrolmentCtUtr(testUtr.uniqueTaxPayerReference)
        val business = result.futureValue
        business mustBe None
      }

      "throw an exception when the connector returns an error" in {
        when(mockConnector.organisationWithUtr(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegisterOrganisationWithIdResponse](InternalServerError))
        val exception = intercept[Exception] {
          testService.getBusinessWithEnrolmentCtUtr(testUtr.uniqueTaxPayerReference).futureValue
        }
        exception.getMessage must include("Unexpected error!")
      }
    }

    "getBusinessWithUserInput should" - {
      "return business details when UserAnswers is complete and connector finds a match" in {
        val expectedRequest = RegisterOrganisationWithIdRequest(
          requiresNameMatch = true,
          IDNumber = testUtr.uniqueTaxPayerReference,
          IDType = "UTR",
          organisationName = Some(orgUkBusinessResponse.organisationName),
          organisationType = Some(testOrgType.code)
        )
        when(mockConnector.organisationWithUtr(eqTo(expectedRequest))(any()))
          .thenReturn(EitherT.rightT[Future, ApiError](orgUkBusinessResponse))
        val result          = testService.getBusinessWithUserInput(organisationUserAnswersUtr)
        val business        = result.futureValue
        business mustBe Some(BusinessDetails(orgUkBusinessResponse.organisationName, orgUkBusinessResponse.address))
        verify(mockConnector).organisationWithUtr(eqTo(expectedRequest))(any())
      }

      "return None when UserAnswers is missing data" in {
        val incompleteUserAnswers = UserAnswers(userAnswersId)
        val result                = testService.getBusinessWithUserInput(incompleteUserAnswers)
        val business              = result.futureValue
        business mustBe None
        verify(mockConnector, never()).organisationWithUtr(any())(any())
      }

      "throw an exception when the connector returns an error" in {
        when(mockConnector.organisationWithUtr(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegisterOrganisationWithIdResponse](InternalServerError))
        val exception = intercept[Exception] {
          testService.getBusinessWithUserInput(organisationUserAnswersUtr).futureValue
        }
        exception.getMessage must include("Unexpected error!")
      }
    }

    "getIndividualByNino method should" - {
      "successfully return an individual's details when the connector returns them successfully" in {
        when(mockConnector.individualWithNino(any[RegisterIndividualWithNinoRequest])(any()))
          .thenReturn(EitherT.rightT[Future, ApiError](testRegisterIndividualWithIdSuccessResponse))

        val result = testService
          .getIndividualByNino(ninoOkFullIndividualResponse, validName, validBirthDate)
          .futureValue

        result mustBe Some(
          IndividualDetails(
            safeId = "testSafeId",
            firstName = "Floriane",
            lastName = "Yammel",
            middleName = Some("Exie"),
            address = testAddress
          )
        )
      }

      "return None when the connector could not get a record match for this user" in {
        when(mockConnector.individualWithNino(any[RegisterIndividualWithNinoRequest])(any()))
          .thenReturn(EitherT.leftT[Future, RegisterIndividualWithIdResponse](NotFoundError))

        val result = testService.getIndividualByNino(ninoNotFound, validName, validBirthDate).futureValue
        result mustBe None
      }

      "throw an 'Unexpected error!' exception when the connector returns an error" in {
        when(mockConnector.individualWithNino(any[RegisterIndividualWithNinoRequest])(any()))
          .thenReturn(EitherT.leftT[Future, RegisterIndividualWithIdResponse](InternalServerError))

        val exception = intercept[Exception] {
          testService.getIndividualByNino(ninoInternalServerError, validName, validBirthDate).futureValue
        }
        exception.getMessage must include("Unexpected error!")
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

        result mustBe Some(
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

      "return None when the connector could not get a record match" in {
        when(mockConnector.individualWithUtr(any[RegisterIndividualWithUtrRequest])(any()))
          .thenReturn(EitherT.leftT[Future, RegisterIndividualWithIdResponse](NotFoundError))

        val result = testService.getIndividualByUtr(individualDetailsUtrUserAnswers).futureValue
        result mustBe None
      }

      "return None when the UserAnswers does not contain a Utr" in {
        val result = testService.getIndividualByUtr(utrNotPresentInIndividualDetailsUserAnswers).futureValue
        result mustBe None
        verify(mockConnector, never()).individualWithUtr(any())(any())
      }

      "return None when the UserAnswers does not contain a Name" in {
        val result = testService.getIndividualByUtr(nameNotPresentIndividualDetailsUserAnswers).futureValue
        result mustBe None
        verify(mockConnector, never()).individualWithUtr(any())(any())
      }

      "throw an 'Unexpected error!' exception when the connector returns an error" in {
        when(mockConnector.individualWithUtr(any[RegisterIndividualWithUtrRequest])(any()))
          .thenReturn(EitherT.leftT[Future, RegisterIndividualWithIdResponse](InternalServerError))
        val exception = intercept[Exception] {
          testService.getIndividualByUtr(individualDetailsUtrUserAnswers).futureValue
        }
        exception.getMessage must include("Unexpected error!")
      }
    }
  }
}
