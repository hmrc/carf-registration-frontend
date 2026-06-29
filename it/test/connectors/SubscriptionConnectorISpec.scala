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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import itutil.ApplicationWithWiremock
import models.SubscriptionId
import models.error.ApiError.{AlreadyRegisteredError, InternalServerError, JsonValidationError, NotFoundError, UnableToProcessSubscriptionError}
import models.requests.{SubscriptionContactDetails, SubscriptionIndividualContact, SubscriptionOrganisationContact, SubscriptionRequest}
import models.responses.*
import org.scalactic.Prettifier.default
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import play.api.http.Status.*
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class SubscriptionConnectorISpec
    extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  val connector: SubscriptionConnector = app.injector.instanceOf[SubscriptionConnector]

  val testSubscriptionId: String = "CARF0000000001"

  val validContactInformation: SubscriptionContactDetails = SubscriptionContactDetails(
    individual = Some(SubscriptionIndividualContact("John", "Doe")),
    organisation = None,
    email = "john.doe@example.com",
    phone = Some("07123456789")
  )

  val validSubscriptionRequest: SubscriptionRequest = SubscriptionRequest(
    gbUser = true,
    idNumber = "SAFE123456",
    idType = "SAFE",
    tradingName = Some("Test Trading Ltd"),
    primaryContact = validContactInformation,
    secondaryContact = Some(
      SubscriptionContactDetails(
        individual = None,
        organisation = Some(SubscriptionOrganisationContact("Org Secondary Contact Ltd")),
        email = "jane.bloggs@example.com",
        phone = Some("07987654321")
      )
    )
  )

  val testSubscriptionDisplayResponse = DisplaySubscriptionResponse(
    success = DisplaySubscriptionSuccess(
      processingDate = "2024-01-25T09:26:17Z",
      carfSubscriptionDetails = DisplaySubscriptionDetails(
        carfReference = testSubscriptionId,
        tradingName = Some("CARF LTD"),
        gbUser = true,
        primaryContact = DisplaySubscriptionContact(
          individual = Some(
            DisplaySubscriptionIndividual(
              firstName = "Joe",
              middleName = None,
              lastName = "Smith"
            )
          ),
          email = "GroupRep@FATCACRS.com",
          phone = Some("01232473743"),
          mobile = Some("07232473743"),
          organisation = None
        ),
        secondaryContact = Some(
          DisplaySubscriptionContact(
            individual = Some(
              DisplaySubscriptionIndividual(
                firstName = "Joe",
                middleName = Some("Martyn"),
                lastName = "Smith"
              )
            ),
            email = "GroupRep@FATCACRS.com",
            phone = Some("01232473744"),
            mobile = Some("07232473744"),
            organisation = None
          )
        )
      )
    )
  )

  val validSubscriptionResponseJson: String = """{"success":{"carfReference":"CARF123456"}}"""

  val testDisplaySubscriptionResponseJson: String =
    """
      |{
      |  "success": {
      |    "processingDate": "2024-01-25T09:26:17Z",
      |    "carfSubscriptionDetails": {
      |      "carfReference": "CARF0000000001",
      |      "tradingName": "CARF LTD",
      |      "gbUser": true,
      |      "primaryContact": {
      |        "individual": {
      |          "firstName": "Joe",
      |          "lastName": "Smith"
      |        },
      |        "email": "GroupRep@FATCACRS.com",
      |        "phone": "01232473743",
      |        "mobile": "07232473743"
      |      },
      |      "secondaryContact": {
      |        "individual": {
      |          "firstName": "Joe",
      |          "middleName": "Martyn",
      |          "lastName": "Smith"
      |        },
      |        "email": "GroupRep@FATCACRS.com",
      |        "phone": "01232473744",
      |        "mobile": "07232473744"
      |      }
      |    }
      |  }
      |}""".stripMargin

  "createSubscription" should {
    "successfully create a subscription and return a subscription ID" in {
      stubFor(
        post(urlPathMatching("/carf-registration/subscription/subscribe"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(validSubscriptionResponseJson)
          )
      )

      val result = connector.createSubscription(validSubscriptionRequest).value.futureValue
      result shouldBe Right(SubscriptionId("CARF123456"))
    }

    "return JsonValidationError when response JSON is invalid" in {
      stubFor(
        post(urlPathMatching("/carf-registration/subscription/subscribe"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson("invalid response").toString)
          )
      )

      val result = connector.createSubscription(validSubscriptionRequest).value.futureValue
      result shouldBe Left(JsonValidationError)
    }

    "return JsonValidationError when response JSON structure is incorrect" in {
      stubFor(
        post(urlPathMatching("/carf-registration/subscription/subscribe"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("""{"incorrect": "structure"}""")
          )
      )

      val result = connector.createSubscription(validSubscriptionRequest).value.futureValue
      result shouldBe Left(JsonValidationError)
    }

    "return AlreadyRegisteredError when backend returns already_registered status" in {
      val errorResponse = Json.obj(
        "status"  -> "already_registered",
        "message" -> "Subscription already exists"
      )

      stubFor(
        post(urlPathMatching("/carf-registration/subscription/subscribe"))
          .willReturn(
            aResponse()
              .withStatus(UNPROCESSABLE_ENTITY)
              .withBody(errorResponse.toString)
          )
      )

      val result = connector.createSubscription(validSubscriptionRequest).value.futureValue
      result shouldBe Left(AlreadyRegisteredError)
    }

    "return AlreadyRegisteredError when backend returns already_registered in error response body" in {
      val errorResponse = Json.obj(
        "status" -> "already_registered"
      )

      stubFor(
        post(urlPathMatching("/carf-registration/subscription/subscribe"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody(errorResponse.toString)
          )
      )

      val result = connector.createSubscription(validSubscriptionRequest).value.futureValue
      result shouldBe Left(AlreadyRegisteredError)
    }

    "return UnableToProcessSubscriptionError when backend returns 400 status" in {
      val errorResponse = Json.obj(
        "status"  -> "Bad request",
        "message" -> "Invalid request"
      )

      stubFor(
        post(urlPathMatching("/carf-registration/subscription/subscribe"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody(errorResponse.toString)
          )
      )

      val result = connector.createSubscription(validSubscriptionRequest).value.futureValue
      result shouldBe Left(UnableToProcessSubscriptionError)
    }

    "return UnableToProcessSubscriptionError when backend returns 500" in {
      stubFor(
        post(urlPathMatching("/carf-registration/subscription/subscribe"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody(Json.obj("message" -> "Internal server error").toString)
          )
      )

      val result = connector.createSubscription(validSubscriptionRequest).value.futureValue
      result shouldBe Left(UnableToProcessSubscriptionError)
    }

    "return UnableToProcessSubscriptionError when backend returns 503" in {
      stubFor(
        post(urlPathMatching("/carf-registration/subscription/subscribe"))
          .willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
              .withBody(Json.obj("message" -> "Service unavailable").toString)
          )
      )

      val result = connector.createSubscription(validSubscriptionRequest).value.futureValue
      result shouldBe Left(UnableToProcessSubscriptionError)
    }

    "return UnableToProcessSubscriptionError when response body is not valid JSON" in {
      stubFor(
        post(urlPathMatching("/carf-registration/subscription/subscribe"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody("Not a JSON response")
          )
      )

      val result = connector.createSubscription(validSubscriptionRequest).value.futureValue
      result shouldBe Left(UnableToProcessSubscriptionError)
    }

    "return UnableToProcessSubscriptionError when response body is empty JSON" in {
      stubFor(
        post(urlPathMatching("/carf-registration/subscription/subscribe"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody("{}")
          )
      )

      val result = connector.createSubscription(validSubscriptionRequest).value.futureValue
      result shouldBe Left(UnableToProcessSubscriptionError)
    }

    "handle subscription request with no secondary contact" in {
      val validRequestWithoutSecondaryContact = validSubscriptionRequest.copy(secondaryContact = None)

      stubFor(
        post(urlPathMatching("/carf-registration/subscription/subscribe"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(validSubscriptionResponseJson)
          )
      )

      val result = connector.createSubscription(validRequestWithoutSecondaryContact).value.futureValue
      result shouldBe Right(SubscriptionId("CARF123456"))
    }

  }
  
  "updateSubscription" should {
    "successfully update a subscription and return a subscription ID" in {
      stubFor(
        put(urlPathMatching("/carf-registration/subscription/amend"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(validSubscriptionResponseJson)
          )
      )

      val result = connector.updateSubscription(validSubscriptionRequest).value.futureValue
      result shouldBe Right(SubscriptionId("CARF123456"))
    }

    "return JsonValidationError when response JSON is invalid" in {
      stubFor(
        put(urlPathMatching("/carf-registration/subscription/amend"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson("invalid response").toString)
          )
      )

      val result = connector.updateSubscription(validSubscriptionRequest).value.futureValue
      result shouldBe Left(JsonValidationError)
    }

    "return JsonValidationError when response JSON structure is incorrect" in {
      stubFor(
        put(urlPathMatching("/carf-registration/subscription/amend"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("""{"incorrect": "structure"}""")
          )
      )

      val result = connector.updateSubscription(validSubscriptionRequest).value.futureValue
      result shouldBe Left(JsonValidationError)
    }

    "return UnableToProcessSubscriptionError when backend returns 400 status" in {
      val testApiErrorDetailResponseJson: String = generateErrorResponseJson("400")

      stubFor(
        put(urlPathMatching("/carf-registration/subscription/subscribe"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody(testApiErrorDetailResponseJson)
          )
      )

      val result = connector.updateSubscription(validSubscriptionRequest).value.futureValue
      result shouldBe Left(UnableToProcessSubscriptionError)
    }

    "return UnableToProcessSubscriptionError when backend returns 500" in {

      val testApiErrorDetailResponseJson: String = generateErrorResponseJson("500")

      stubFor(
        put(urlPathMatching("/carf-registration/subscription/amend"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody(testApiErrorDetailResponseJson)
          )
      )

      val result = connector.updateSubscription(validSubscriptionRequest).value.futureValue
      result shouldBe Left(UnableToProcessSubscriptionError)
    }

    "return UnableToProcessSubscriptionError when response body is not valid JSON" in {
      stubFor(
        put(urlPathMatching("/carf-registration/subscription/amend"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody("Not a JSON response")
          )
      )

      val result = connector.updateSubscription(validSubscriptionRequest).value.futureValue
      result shouldBe Left(UnableToProcessSubscriptionError)
    }

    "return UnableToProcessSubscriptionError when response body is empty JSON" in {
      stubFor(
        put(urlPathMatching("/carf-registration/subscription/amend"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody("{}")
          )
      )

      val result = connector.updateSubscription(validSubscriptionRequest).value.futureValue
      result shouldBe Left(UnableToProcessSubscriptionError)
    }

    "handle subscription request with no secondary contact" in {
      val validRequestWithoutSecondaryContact = validSubscriptionRequest.copy(secondaryContact = None)

      stubFor(
        put(urlPathMatching("/carf-registration/subscription/amend"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(validSubscriptionResponseJson)
          )
      )

      val result = connector.updateSubscription(validRequestWithoutSecondaryContact).value.futureValue
      result shouldBe Right(SubscriptionId("CARF123456"))
    }
  }

  "displaySubscription" should {

    val baseUrlPattern = s"/carf-registration/subscription/display/.*"

    "successfully retrieve a DisplaySubscriptionResponse" in {
      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(testDisplaySubscriptionResponseJson)
          )
      )

      val result = connector.displaySubscription(testSubscriptionId).value.futureValue
      result shouldBe Right(testSubscriptionDisplayResponse)
    }

    "return JsonValidationError when response JSON is invalid" in {
      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson("invalid response").toString)
          )
      )

      val result = connector.displaySubscription(testSubscriptionId).value.futureValue
      result shouldBe Left(JsonValidationError)
    }

    "return JsonValidationError when response JSON structure is incorrect" in {
      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("""{"incorrect": "structure"}""")
          )
      )

      val result = connector.displaySubscription(testSubscriptionId).value.futureValue
      result shouldBe Left(JsonValidationError)
    }

    "return NotFoundError when backend returns 404" in {
      val errorResponse = Json.obj(
        "status"  -> "Not Found",
        "message" -> "Not Found"
      )

      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody(errorResponse.toString)
          )
      )

      val result = connector.displaySubscription(testSubscriptionId).value.futureValue
      result shouldBe Left(NotFoundError)
    }

    "return InternalServerError when backend returns 400" in {
      val errorResponse = Json.obj(
        "status"  -> "Bad request",
        "message" -> "Invalid request"
      )

      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody(errorResponse.toString)
          )
      )

      val result = connector.displaySubscription(testSubscriptionId).value.futureValue
      result shouldBe Left(InternalServerError)
    }

    "return InternalServerError when backend returns 422" in {
      val errorResponse = Json.obj(
        "status"  -> "Unprocessable Entity",
        "message" -> "Invalid ID"
      )

      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(UNPROCESSABLE_ENTITY)
              .withBody(errorResponse.toString)
          )
      )

      val result = connector.displaySubscription(testSubscriptionId).value.futureValue
      result shouldBe Left(InternalServerError)
    }

    "return InternalServerError when backend returns 500" in {
      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody(Json.obj("message" -> "Internal server error").toString)
          )
      )

      val result = connector.displaySubscription(testSubscriptionId).value.futureValue
      result shouldBe Left(InternalServerError)
    }

    "return InternalServerError when backend returns 503" in {
      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
              .withBody(Json.obj("message" -> "Service unavailable").toString)
          )
      )

      val result = connector.displaySubscription(testSubscriptionId).value.futureValue
      result shouldBe Left(InternalServerError)
    }

  }

  def generateErrorResponseJson(errorCode: String): String =
    s"""{
      |  "errorDetail": {
      |    "errorCode": "$errorCode",
      |    "errorMessage": "Test Error Message",
      |    "source": "Test",
      |    "sourceFaultDetail": {
      |      "detail": [
      |        "Test Error Detail"
      |      ]
      |    },
      |    "timestamp": "2020-09-25T21:54:12.015Z",
      |    "correlationId": "1ae81b45-41b4-4642-ae1c-db1126900001"
      |  }
      |}""".stripMargin
}
