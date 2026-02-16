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
import models.error.ApiError
import models.requests.SearchByPostcodeRequest
import models.responses.{AddressRecord, AddressResponse, CountryRecord}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

class AddressLookupConnectorISpec
    extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val connector: AddressLookupConnector = app.injector.instanceOf[AddressLookupConnector]

  val searchByPostcodeValidResponse: Seq[AddressResponse] = Seq(
    AddressResponse(
      id = "Test-Id",
      address = AddressRecord(
        lines = List("Address-Line1", "Address-Line2"),
        town = "Bristol",
        postcode = "BS6 1XX",
        country = CountryRecord(code = "UK", name = "United Kingdom")
      )
    )
  )

  val validRequestBody: SearchByPostcodeRequest = SearchByPostcodeRequest(
    postcode = "BS6 1XX",
    filter = Some("1")
  )

  "searchByPostcode" should {
    "successfully retrieve a list of addresses" in {
      stubFor(
        post(urlPathMatching("/address-lookup/lookup"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(searchByPostcodeValidResponse).toString)
          )
      )

      val result = connector.searchByPostcode(validRequestBody).value.futureValue

      result shouldBe Right(searchByPostcodeValidResponse)
    }

    "return a Json validation error if unexpected response is returned from the other service" in {
      stubFor(
        post(urlPathMatching("/address-lookup/lookup"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson("invalid response").toString)
          )
      )

      val result = connector.searchByPostcode(validRequestBody).value.futureValue

      result shouldBe Left(ApiError.JsonValidationError)
    }

    "return an internal server error if a non 200 response is returned by the other service" in {
      stubFor(
        post(urlPathMatching("/address-lookup/lookup"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody(Json.toJson("test_body").toString)
          )
      )

      val result = connector.searchByPostcode(validRequestBody).value.futureValue

      result shouldBe Left(ApiError.InternalServerError)
    }
  }
}
