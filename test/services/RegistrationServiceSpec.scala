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
import models.requests.RegisterOrganisationWithIdRequest
import models.responses.{RegisterIndividualWithIdResponse, RegisterOrganisationWithIdResponse}
import models.{Address, BusinessDetails, IndividualDetails, Name, OrganisationRegistrationType, UniqueTaxpayerReference, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalactic.Prettifier.default
import pages.{NiNumberPage, OrganisationRegistrationTypePage, RegisterDateOfBirthPage, WhatIsTheNameOfYourBusinessPage, WhatIsYourNamePage, YourUniqueTaxpayerReferencePage}

import java.time.LocalDate
import scala.concurrent.Future

class RegistrationServiceSpec extends SpecBase {
  val mockConnector: RegistrationConnector = mock[RegistrationConnector]
  val testService                          = new RegistrationService(mockConnector)
  val testOrgType                          = OrganisationRegistrationType.LimitedCompany
  val testNino                             = "JX123456D"
  val birthDate: LocalDate                 = LocalDate.of(2000, 1, 1)

  val testAddress = Address(
    addressLine1 = "123 Main Street",
    addressLine2 = Some("Birmingham"),
    addressLine3 = None,
    addressLine4 = None,
    postalCode = Some("B23 2AZ"),
    countryCode = "GB"
  )

  val testRegisterIndividualWithIdSuccessResponse: RegisterIndividualWithIdResponse = RegisterIndividualWithIdResponse(
    safeId = "testSafeId",
    firstName = "Floriane",
    lastName = "Yammel",
    middleName = Some("Exie"),
    address = testAddress
  )
  val userAnswersIndividualWithNino                                                 = UserAnswers(userAnswersId)
    .set(
      WhatIsYourNamePage,
      Name(testRegisterIndividualWithIdSuccessResponse.firstName, testRegisterIndividualWithIdSuccessResponse.lastName)
    )
    .success
    .value
    .set(NiNumberPage, testNino)
    .success
    .value
    .set(RegisterDateOfBirthPage, birthDate)
    .success
    .value

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

  val orgNonUkBusinessResponse = RegisterOrganisationWithIdResponse(
    safeId = "testSafeId",
    code = Some("0001"),
    organisationName = "International Ltd",
    address = Address(
      addressLine1 = "3 Apple Street",
      addressLine2 = Some("New York"),
      addressLine3 = None,
      addressLine4 = None,
      postalCode = Some("11722"),
      countryCode = "US"
    )
  )

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

        val result   = testService.getBusinessWithEnrolmentCtUtr(testUtr.uniqueTaxPayerReference)
        val business = result.futureValue

        business          mustBe defined
        business.get.name mustBe orgUkBusinessResponse.organisationName
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

      val userAnswers = UserAnswers(userAnswersId)
        .set(YourUniqueTaxpayerReferencePage, UniqueTaxpayerReference(testUtr.uniqueTaxPayerReference))
        .success
        .value
        .set(WhatIsTheNameOfYourBusinessPage, orgUkBusinessResponse.organisationName)
        .success
        .value
        .set(OrganisationRegistrationTypePage, testOrgType)
        .success
        .value

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

        val result   = testService.getBusinessWithUserInput(userAnswers)
        val business = result.futureValue

        business          mustBe defined
        business.get.name mustBe orgUkBusinessResponse.organisationName
        verify(mockConnector).organisationWithUtr(eqTo(expectedRequest))(any())
      }

      "return None when UserAnswers is missing data" in {
        val incompleteUserAnswers = UserAnswers(userAnswersId)

        val result   = testService.getBusinessWithUserInput(incompleteUserAnswers)
        val business = result.futureValue

        business mustBe None
      }

      "throw an exception when the connector returns an error" in {
        val userAnswers = UserAnswers(userAnswersId)
          .set(YourUniqueTaxpayerReferencePage, UniqueTaxpayerReference(testUtr.uniqueTaxPayerReference))
          .success
          .value
          .set(WhatIsTheNameOfYourBusinessPage, orgUkBusinessResponse.organisationName)
          .success
          .value
          .set(OrganisationRegistrationTypePage, testOrgType)
          .success
          .value

        when(mockConnector.organisationWithUtr(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegisterOrganisationWithIdResponse](InternalServerError))

        val exception = intercept[Exception] {
          testService.getBusinessWithUserInput(userAnswers).futureValue
        }

        exception.getMessage must include("Unexpected error!")
      }
    }

    // zxc

    "getIndividualByNino method should" - {
      "successfully return an individual's details when the connector returns them successfully" in {

        when(mockConnector.individualWithNino(any())(any()))
          .thenReturn(EitherT.rightT[Future, ApiError](testRegisterIndividualWithIdSuccessResponse))

        val expectedResult = IndividualDetails(
          safeId = "testSafeId",
          firstName = "Floriane",
          lastName = "Yammel",
          middleName = Some("Exie"),
          address = testAddress
        )

        val result = testService.getIndividualByNino("testInput", userAnswersIndividualWithNino).futureValue
        result mustBe Some(expectedResult)
      }

      "return none when the connector could not get a business partner record match for this user" in {
        when(mockConnector.individualWithNino(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegisterIndividualWithIdResponse](NotFoundError))

        val result = testService.getIndividualByNino("testInput", userAnswersIndividualWithNino).futureValue

        result mustBe None
      }
      // TODO: Change below test in CARF-166 to handle scenario gracefully (redirect to journey recovery)
      "throw an exception when the connector returns an error" in {
        when(mockConnector.individualWithNino(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegisterIndividualWithIdResponse](InternalServerError))

        val exception = intercept[Exception] {
          testService.getIndividualByNino("testInput", userAnswersIndividualWithNino).futureValue
        }

        exception.getMessage must include("Unexpected Error!")
      }
    }
  }
}
