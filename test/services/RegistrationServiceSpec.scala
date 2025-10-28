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
import models.responses.RegisterIndividualWithIdResponse
import models.{Address, BusinessDetails, IndividualDetails}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}

import scala.concurrent.Future

class RegistrationServiceSpec extends SpecBase {

  val mockConnector: RegistrationConnector = mock[RegistrationConnector]
  val testService                          = new RegistrationService(mockConnector)

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

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
  }
  "RegistrationService" - {
    "getBusinessByUtr method should" - {
      "return UK business for UTR starting with '1'" in {
        val result = testService.getBusinessByUtr("1234567890", None)

        val business = result.futureValue
        business                          mustBe defined
        business.get.name                 mustBe "Agent ABC Ltd"
        business.get.isUkBased            mustBe true
        business.get.address.addressLine1 mustBe "2 High Street"
        business.get.address.addressLine2 mustBe Some("Birmingham")
        business.get.address.postalCode   mustBe Some("B23 2AZ")
        business.get.address.countryCode  mustBe "GB"
      }

      "return Non-UK business for UTR starting with '2'" in {
        val result = testService.getBusinessByUtr("2987654321", Some("International Ltd"))

        val business = result.futureValue
        business                          mustBe defined
        business.get.name                 mustBe "International Ltd"
        business.get.isUkBased            mustBe false
        business.get.address.addressLine1 mustBe "3 Apple Street"
        business.get.address.addressLine2 mustBe Some("New York")
        business.get.address.postalCode   mustBe Some("11722")
        business.get.address.countryCode  mustBe "US"
      }

      "return a business when UTR and businessName has been provided" in {
        val result =
          testService.getBusinessByUtr(utr = "1234567890", name = Some("Agent ABC Ltd"))

        val business = result.futureValue

        business                          mustBe defined
        business.get.name                 mustBe "Agent ABC Ltd"
        business.get.isUkBased            mustBe true
        business.get.address.addressLine1 mustBe "2 High Street"
        business.get.address.addressLine2 mustBe Some("Birmingham")
      }

      "return None when business cannot be found by UTR" in {
        val result = testService.getBusinessByUtr("9999999999", None)

        val business = result.futureValue
        business mustBe None
      }
    }
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

        val result = testService.getIndividualByNino("testInput").futureValue

        result mustBe Some(expectedResult)
      }
      "return none when the connector could not get a business partner record match for this user" in {
        when(mockConnector.individualWithNino(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegisterIndividualWithIdResponse](NotFoundError))

        val result = testService.getIndividualByNino("testInput").futureValue

        result mustBe None
      }
      // TODO: Change below test in CARF-166 to handle scenario gracefully (redirect to journey recovery)
      "throw an exception when the connector returns an error" in {
        when(mockConnector.individualWithNino(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegisterIndividualWithIdResponse](InternalServerError))

        val exception = intercept[Exception] {
          testService.getIndividualByNino("testInput").futureValue
        }

        exception.getMessage must include("Unexpected Error!")
      }
    }
  }
}
