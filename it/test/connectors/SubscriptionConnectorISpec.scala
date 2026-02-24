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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlPathMatching}
import itutil.ApplicationWithWiremock
import models.SubscriptionId
import models.error.ApiError.{AlreadyRegisteredError, JsonValidationError, UnableToCreateEMTPSubscriptionError}
import models.requests.{CreateSubscriptionRequest, SubscriptionContactDetails, SubscriptionIndividualContact, SubscriptionOrganisationContact}
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

  val validContactInformation: SubscriptionContactDetails = SubscriptionContactDetails(
    individual = Some(SubscriptionIndividualContact("John", "Doe")),
    organisation = None,
    email = "john.doe@example.com",
    phone = Some("07123456789")
  )

  val validSubscriptionRequest: CreateSubscriptionRequest = CreateSubscriptionRequest(
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

  val validSubscriptionResponseJson: String = """{"success":{"CARFReference":"CARF123456"}}"""

  "createSubscription" should {
    "successfully create a subscription and return a subscription ID" in {
      stubFor(
        post(urlPathMatching("/carf-registration/subscription/subscribe"))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
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
              .withStatus(CREATED)
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
              .withStatus(CREATED)
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

    "return UnableToCreateEMTPSubscriptionError when backend returns 400 status" in {
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
      result shouldBe Left(UnableToCreateEMTPSubscriptionError)
    }

    "return UnableToCreateEMTPSubscriptionError when backend returns 500" in {
      stubFor(
        post(urlPathMatching("/carf-registration/subscription/subscribe"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody(Json.obj("message" -> "Internal server error").toString)
          )
      )

      val result = connector.createSubscription(validSubscriptionRequest).value.futureValue
      result shouldBe Left(UnableToCreateEMTPSubscriptionError)
    }

    "return UnableToCreateEMTPSubscriptionError when backend returns 503" in {
      stubFor(
        post(urlPathMatching("/carf-registration/subscription/subscribe"))
          .willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
              .withBody(Json.obj("message" -> "Service unavailable").toString)
          )
      )

      val result = connector.createSubscription(validSubscriptionRequest).value.futureValue
      result shouldBe Left(UnableToCreateEMTPSubscriptionError)
    }

    "return UnableToCreateEMTPSubscriptionError when response body is not valid JSON" in {
      stubFor(
        post(urlPathMatching("/carf-registration/subscription/subscribe"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody("Not a JSON response")
          )
      )

      val result = connector.createSubscription(validSubscriptionRequest).value.futureValue
      result shouldBe Left(UnableToCreateEMTPSubscriptionError)
    }

    "return UnableToCreateEMTPSubscriptionError when response body is empty JSON" in {
      stubFor(
        post(urlPathMatching("/carf-registration/subscription/subscribe"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody("{}")
          )
      )

      val result = connector.createSubscription(validSubscriptionRequest).value.futureValue
      result shouldBe Left(UnableToCreateEMTPSubscriptionError)
    }

    "handle subscription request with no secondary contact" in {
      val validRequestWithoutSecondaryContact = validSubscriptionRequest.copy(secondaryContact = None)

      stubFor(
        post(urlPathMatching("/carf-registration/subscription/subscribe"))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
              .withBody(validSubscriptionResponseJson)
          )
      )

      val result = connector.createSubscription(validRequestWithoutSecondaryContact).value.futureValue
      result shouldBe Right(SubscriptionId("CARF123456"))
    }

  }
}
