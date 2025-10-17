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

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.http.HeaderCarrier
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlPathMatching}
import itutil.ApplicationWithWiremock
import models.Address
import models.requests.RegisterIndividualWithIdRequest
import models.responses.RegisterIndividualWithIdResponse
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

class RegistrationConnectorISpec
    extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val connector: RegistrationConnector = app.injector.instanceOf[RegistrationConnector]

  val validRequestBody: RegisterIndividualWithIdRequest = RegisterIndividualWithIdRequest(
    requiresNameMatch = true,
    IDNumber = "testIDNumber",
    IDType = "testIDType",
    dateOfBirth = "testDob",
    firstName = "testFirstName",
    lastName = "testLastName"
  )

  val addressResponse: Address = Address(
    addressLine1 = "2 High Street",
    addressLine2 = Some("Birmingham"),
    addressLine3 = Some("Townington"),
    addressLine4 = Some("Kingdom of UK"),
    postalCode = Some("B23 2AZ"),
    countryCode = "GB"
  )
  
  val validResponse: RegisterIndividualWithIdResponse = RegisterIndividualWithIdResponse(
    safeId = "testSafeId",
    firstName = "testFirstName",
    lastName = "testLastName",
    middleName = Some("TestMiddleName"),
    address = addressResponse
  )
  
  "individualWithNino" should {
    "successfully retrieve a name and address" in {
      stubFor(
        post(urlPathMatching("/carf-registration/individual/nino"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(validResponse).toString)
          )
      )

      val result = connector.individualWithNino(validRequestBody).value.futureValue

      result shouldBe Right(validResponse)
    }
  }
}
