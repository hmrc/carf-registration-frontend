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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlPathMatching}
import itutil.ApplicationWithWiremock
import models.Address
import models.error.ApiError
import models.requests.*
import models.responses.{AddressRegistrationResponse, RegisterIndividualWithIdResponse, RegisterOrganisationWithIdResponse}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

class RegistrationConnectorISpec
    extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val connector: RegistrationConnector = app.injector.instanceOf[RegistrationConnector]

  val addressResponse: AddressRegistrationResponse = AddressRegistrationResponse(
    addressLine1 = "2 High Street",
    addressLine2 = Some("Birmingham"),
    addressLine3 = Some("Townington"),
    addressLine4 = Some("Kingdom of UK"),
    postalCode = Some("B23 2AZ"),
    countryCode = "GB"
  )

  val validIndWithNinoRequestBody: RegisterIndividualWithNinoRequest = RegisterIndividualWithNinoRequest(
    requiresNameMatch = true,
    IDNumber = "testIDNumber",
    IDType = "testIDType",
    dateOfBirth = "testDob",
    firstName = "testFirstName",
    lastName = "testLastName"
  )

  val validIndWithUtrRequestBody: RegisterIndividualWithUtrRequest = RegisterIndividualWithUtrRequest(
    requiresNameMatch = true,
    IDNumber = "testIDNumber",
    IDType = "testIDType",
    firstName = "testFirstName",
    lastName = "testLastName"
  )

  val validIndividualResponse: RegisterIndividualWithIdResponse = RegisterIndividualWithIdResponse(
    safeId = "testSafeId",
    firstName = "testFirstName",
    lastName = "testLastName",
    middleName = Some("TestMiddleName"),
    address = addressResponse
  )

  val validOrganisationNonAutoMatchRequestBody: RegOrgWithIdNonAutoMatchRequest = RegOrgWithIdNonAutoMatchRequest(
    requiresNameMatch = true,
    IDNumber = "testIDNumber",
    IDType = "testIDType",
    organisationName = "Monsters Inc",
    organisationType = "0001"
  )

  val validOrganisationCTAutoMatchRequestBody: RegOrgWithIdCTAutoMatchRequest = RegOrgWithIdCTAutoMatchRequest(
    requiresNameMatch = false,
    IDNumber = "testIDNumber",
    IDType = "0002"
  )

  val validOrganisationResponse: RegisterOrganisationWithIdResponse = RegisterOrganisationWithIdResponse(
    safeId = "testSafeId",
    code = Some("0001"),
    organisationName = "Monsters Inc",
    address = addressResponse
  )

  "individualWithNino" should {
    "successfully retrieve a name and address" in {
      stubFor(
        post(urlPathMatching("/carf-registration/individual/nino"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(validIndividualResponse).toString)
          )
      )
      val result = connector.individualWithNino(validIndWithNinoRequestBody).value.futureValue
      result shouldBe Right(validIndividualResponse)
    }

    "return a Json validation error if unexpected response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/carf-registration/individual/nino"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson("invalid response").toString)
          )
      )
      val result = connector.individualWithNino(validIndWithNinoRequestBody).value.futureValue
      result shouldBe Left(ApiError.JsonValidationError)
    }

    "return a not found error if 404 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/carf-registration/individual/nino"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody(Json.toJson("test_body").toString)
          )
      )
      val result = connector.individualWithNino(validIndWithNinoRequestBody).value.futureValue
      result shouldBe Left(ApiError.NotFoundError)
    }

    "return an internal server error if 500 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/carf-registration/individual/nino"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody(Json.toJson("test_body").toString)
          )
      )
      val result = connector.individualWithNino(validIndWithNinoRequestBody).value.futureValue
      result shouldBe Left(ApiError.InternalServerError)
    }
  }

  "individualWithUtr" should {
    "successfully retrieve a Sole Trader's first and last name and address" in {
      stubFor(
        post(urlPathMatching("/carf-registration/individual/utr"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(validIndividualResponse).toString)
          )
      )
      val result = connector.individualWithUtr(validIndWithUtrRequestBody).value.futureValue
      result shouldBe Right(validIndividualResponse)
    }

    "return a Json validation error if unexpected response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/carf-registration/individual/utr"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson("invalid response").toString)
          )
      )
      val result = connector.individualWithUtr(validIndWithUtrRequestBody).value.futureValue
      result shouldBe Left(ApiError.JsonValidationError)
    }

    "return a not found error if 404 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/carf-registration/individual/utr"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody(Json.toJson("test_body").toString)
          )
      )
      val result = connector.individualWithUtr(validIndWithUtrRequestBody).value.futureValue
      result shouldBe Left(ApiError.NotFoundError)
    }

    "return an internal server error if 500 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/carf-registration/individual/utr"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody(Json.toJson("test_body").toString)
          )
      )
      val result = connector.individualWithUtr(validIndWithUtrRequestBody).value.futureValue
      result shouldBe Left(ApiError.InternalServerError)
    }
  }

  "organisationWithUtrNonAutoMatch" should {
    "successfully retrieve a name and address" in {
      stubFor(
        post(urlPathMatching("/carf-registration/organisation/utr/user-entered"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(validOrganisationResponse).toString)
          )
      )

      val result = connector.organisationWithUtrNonAutoMatch(validOrganisationNonAutoMatchRequestBody).value.futureValue

      result shouldBe Right(validOrganisationResponse)
    }

    "return a Json validation error if unexpected response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/carf-registration/organisation/utr/user-entered"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson("invalid response").toString)
          )
      )

      val result = connector.organisationWithUtrNonAutoMatch(validOrganisationNonAutoMatchRequestBody).value.futureValue

      result shouldBe Left(ApiError.JsonValidationError)
    }

    "return a not found error if 404 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/carf-registration/organisation/utr/user-entered"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody(Json.toJson("test_body").toString)
          )
      )

      val result = connector.organisationWithUtrNonAutoMatch(validOrganisationNonAutoMatchRequestBody).value.futureValue

      result shouldBe Left(ApiError.NotFoundError)
    }

    "return an internal server error if 500 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/carf-registration/organisation/utr/user-entered"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody(Json.toJson("test_body").toString)
          )
      )

      val result = connector.organisationWithUtrNonAutoMatch(validOrganisationNonAutoMatchRequestBody).value.futureValue

      result shouldBe Left(ApiError.InternalServerError)
    }
  }

  "organisationWithUtrCTAutoMatch" should {
    "successfully retrieve a name and address" in {
      stubFor(
        post(urlPathMatching("/carf-registration/organisation/utr/ct-auto-match"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(validOrganisationResponse).toString)
          )
      )

      val result = connector.organisationWithUtrCTAutoMatch(validOrganisationCTAutoMatchRequestBody).value.futureValue

      result shouldBe Right(validOrganisationResponse)
    }

    "return a Json validation error if unexpected response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/carf-registration/organisation/utr/ct-auto-match"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson("invalid response").toString)
          )
      )

      val result = connector.organisationWithUtrCTAutoMatch(validOrganisationCTAutoMatchRequestBody).value.futureValue

      result shouldBe Left(ApiError.JsonValidationError)
    }

    "return a not found error if 404 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/carf-registration/organisation/utr/ct-auto-match"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody(Json.toJson("test_body").toString)
          )
      )

      val result = connector.organisationWithUtrCTAutoMatch(validOrganisationCTAutoMatchRequestBody).value.futureValue

      result shouldBe Left(ApiError.NotFoundError)
    }

    "return an internal server error if 500 status response is returned from backend" in {
      stubFor(
        post(urlPathMatching("/carf-registration/organisation/utr/ct-auto-match"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody(Json.toJson("test_body").toString)
          )
      )

      val result = connector.organisationWithUtrCTAutoMatch(validOrganisationCTAutoMatchRequestBody).value.futureValue

      result shouldBe Left(ApiError.InternalServerError)
    }
  }

}
