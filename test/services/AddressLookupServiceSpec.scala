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
import config.FrontendAppConfig
import connectors.AddressLookupConnector
import controllers.routes
import forms.individualWithoutId.IndFindAddressFormProvider
import models.error.ApiError
import models.requests.SearchByPostcodeRequest
import models.responses.{AddressRecord, AddressResponse, CountryRecord}
import models.{IndFindAddress, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.individualWithoutId.IndFindAddressPage
import play.api.Logging
import play.api.data.Form
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import views.html.individualWithoutId.IndFindAddressView

import scala.concurrent.{ExecutionContext, Future}

class AddressLookupServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private implicit val hc: HeaderCarrier    = HeaderCarrier()

  val mockConnector: AddressLookupConnector = mock[AddressLookupConnector]
  val service                               = new AddressLookupService(mockConnector)

  val sampleAddresses: Seq[AddressResponse] = Seq(
    AddressResponse(
      id = "123",
      address = AddressRecord(
        lines = List("1 Test Street"),
        town = "Test Town",
        postcode = "TE1 1ST",
        country = CountryRecord(code = "UK", name = "United Kingdom")
      )
    ),
    AddressResponse(
      id = "124",
      address = AddressRecord(
        lines = List("2 Test Street"),
        town = "Test Town",
        postcode = "TE1 1ST",
        country = CountryRecord(code = "UK", name = "United Kingdom")
      )
    )
  )

  val singleAddress: Seq[AddressResponse] = sampleAddresses.take(1)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAddressLookupConnector)
  }

  "AddressSearchService" - {

    "postcodeSearch" - {

      "must return addresses when initial search with property filter succeeds" in {
        val service = new AddressLookupService(mockAddressLookupConnector)

        when(
          mockAddressLookupConnector
            .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", Some("Flat 1"))))(any())
        )
          .thenReturn(Future.successful(Right(singleAddress)))

        val result = service.postcodeSearch("TE1 1ST", Some("Flat 1")).futureValue

        result mustEqual singleAddress
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", Some("Flat 1"))))(any())
      }

      "must return addresses when initial search without property filter succeeds" in {
        val service = new AddressLookupService(mockAddressLookupConnector)

        when(mockAddressLookupConnector.searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any()))
          .thenReturn(Future.successful(Right(sampleAddresses)))

        val result = service.postcodeSearch("TE1 1ST", None).futureValue

        result mustEqual sampleAddresses
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any())
      }

      "must retry without filter when initial search with property filter returns empty results" in {
        val service = new AddressLookupService(mockAddressLookupConnector)

        when(
          mockAddressLookupConnector
            .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", Some("Flat 99"))))(any())
        )
          .thenReturn(Future.successful(Right(Seq.empty)))
        when(mockAddressLookupConnector.searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any()))
          .thenReturn(Future.successful(Right(sampleAddresses)))

        val result = service.postcodeSearch("TE1 1ST", Some("Flat 99")).futureValue

        result mustEqual sampleAddresses
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", Some("Flat 99"))))(any())
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any())
      }

      "must return empty sequence when initial search with property filter returns empty and fallback also returns empty" in {
        val service = new AddressLookupService(mockAddressLookupConnector)

        when(
          mockAddressLookupConnector
            .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", Some("Flat 99"))))(any())
        )
          .thenReturn(Future.successful(Right(Seq.empty)))
        when(mockAddressLookupConnector.searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any()))
          .thenReturn(Future.successful(Right(Seq.empty)))

        val result = service.postcodeSearch("TE1 1ST", Some("Flat 99")).futureValue

        result mustEqual Seq.empty
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", Some("Flat 99"))))(any())
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any())
      }

      "must return empty sequence when initial search without property filter returns empty results" in {
        val service = new AddressLookupService(mockAddressLookupConnector)

        when(mockAddressLookupConnector.searchByPostcode(eqTo(SearchByPostcodeRequest("XX1 1XX", None)))(any()))
          .thenReturn(Future.successful(Right(Seq.empty)))

        val result = service.postcodeSearch("XX1 1XX", None).futureValue

        result mustEqual Seq.empty
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("XX1 1XX", None)))(any())
      }

      "must not retry when initial search without property filter returns empty results" in {
        val service = new AddressLookupService(mockAddressLookupConnector)

        when(mockAddressLookupConnector.searchByPostcode(eqTo(SearchByPostcodeRequest("XX1 1XX", None)))(any()))
          .thenReturn(Future.successful(Right(Seq.empty)))

        service.postcodeSearch("XX1 1XX", None).futureValue

        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(any())(any())
      }

      "must return empty sequence when initial search returns API error" in {
        val service = new AddressLookupService(mockAddressLookupConnector)

        val apiError = ApiError.BadRequestError
        when(mockAddressLookupConnector.searchByPostcode(eqTo(SearchByPostcodeRequest("INVALID", None)))(any()))
          .thenReturn(Future.successful(Left(apiError)))

        val result = service.postcodeSearch("INVALID", None).futureValue

        result mustEqual Seq.empty
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("INVALID", None)))(any())
      }

      "must return empty sequence when initial search with property filter returns API error" in {
        val service = new AddressLookupService(mockAddressLookupConnector)

        val apiError = ApiError.BadRequestError
        when(
          mockAddressLookupConnector
            .searchByPostcode(eqTo(SearchByPostcodeRequest("INVALID", Some("Flat 1"))))(any())
        )
          .thenReturn(Future.successful(Left(apiError)))

        val result = service.postcodeSearch("INVALID", Some("Flat 1")).futureValue

        result mustEqual Seq.empty
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("INVALID", Some("Flat 1"))))(any())
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(any())(any())
      }

      "must handle connector failure gracefully" in {
        val service = new AddressLookupService(mockAddressLookupConnector)

        when(mockAddressLookupConnector.searchByPostcode(any())(any()))
          .thenReturn(Future.failed(new RuntimeException("Connection failed")))

        val result = service.postcodeSearch("TE1 1ST", None).failed.futureValue

        result               mustBe a[RuntimeException]
        result.getMessage mustEqual "Connection failed"
      }

      "must return empty sequence when initial search with property filter returns empty and fallback returns API error" in {
        val service = new AddressLookupService(mockAddressLookupConnector)

        val apiError = ApiError.BadRequestError
        when(
          mockAddressLookupConnector
            .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", Some("Flat 555"))))(any())
        )
          .thenReturn(Future.successful(Right(Seq.empty)))
        when(mockAddressLookupConnector.searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any()))
          .thenReturn(Future.successful(Left(apiError)))

        val result = service.postcodeSearch("TE1 1ST", Some("Flat 555")).futureValue

        result mustEqual Seq.empty
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", Some("Flat 555"))))(any())
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any())
      }

    }
  }
}
