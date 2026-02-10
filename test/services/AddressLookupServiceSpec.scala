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
import connectors.AddressLookupConnector
import generators.Generators
import models.error.ApiError
import models.requests.SearchByPostcodeRequest
import models.responses.{AddressRecord, AddressResponse, CountryRecord}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class AddressLookupServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with Generators {

  val mockAddressLookupConnector: AddressLookupConnector = mock[AddressLookupConnector]

  val service: AddressLookupService = new AddressLookupService(mockAddressLookupConnector)

  val sampleAddresses: Seq[AddressResponse] = Seq(
    AddressResponse(
      id = "123",
      address = AddressRecord(
        lines = List("1 Test Street"),
        town = "Test Town",
        postcode = validPostcodes.sample.value,
        country = CountryRecord(code = "UK", name = "United Kingdom")
      )
    ),
    AddressResponse(
      id = "124",
      address = AddressRecord(
        lines = List("2 Test Street"),
        town = "Test Town",
        postcode = validPostcodes.sample.value,
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

      "must return address when initial search with property filter succeeds" in {

        when(
          mockAddressLookupConnector
            .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", Some("Flat 1"))))(any())
        )
          .thenReturn(Future.successful(Right(singleAddress)))

        val result = service.postcodeSearch("TE1 1ST", Some("Flat 1")).futureValue

        result mustEqual Right(singleAddress)
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", Some("Flat 1"))))(any())
      }

      "must return addresses when initial search without property filter succeeds" in {

        when(mockAddressLookupConnector.searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any()))
          .thenReturn(Future.successful(Right(sampleAddresses)))

        val result = service.postcodeSearch("TE1 1ST", None).futureValue

        result mustEqual Right(sampleAddresses)
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any())
      }

      "must return addresses when retrying without filter after initial search returns empty results" in {
        when(
          mockAddressLookupConnector
            .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", Some("Flat 99"))))(any())
        )
          .thenReturn(Future.successful(Right(Seq.empty)))
        when(mockAddressLookupConnector.searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any()))
          .thenReturn(Future.successful(Right(sampleAddresses)))

        val result = service.postcodeSearch("TE1 1ST", Some("Flat 99")).futureValue

        result mustEqual Right(sampleAddresses)
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", Some("Flat 99"))))(any())
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any())
      }

      "must return empty sequence when initial search with property filter returns empty and fallback also returns empty" in {

        when(
          mockAddressLookupConnector
            .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", Some("Flat 99"))))(any())
        )
          .thenReturn(Future.successful(Right(Seq.empty)))
        when(mockAddressLookupConnector.searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any()))
          .thenReturn(Future.successful(Right(Seq.empty)))

        val result = service.postcodeSearch("TE1 1ST", Some("Flat 99")).futureValue

        result mustEqual Right(Seq.empty)
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", Some("Flat 99"))))(any())
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any())
      }

      "must return empty sequence when initial search without property filter returns empty results" in {

        when(mockAddressLookupConnector.searchByPostcode(eqTo(SearchByPostcodeRequest("XX1 1XX", None)))(any()))
          .thenReturn(Future.successful(Right(Seq.empty)))

        val result = service.postcodeSearch("XX1 1XX", None).futureValue

        result mustEqual Right(Seq.empty)
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("XX1 1XX", None)))(any())
      }

      "must return API error when initial search returns API error" in {

        val apiError = ApiError.BadRequestError
        when(mockAddressLookupConnector.searchByPostcode(eqTo(SearchByPostcodeRequest("INVALID", None)))(any()))
          .thenReturn(Future.successful(Left(apiError)))

        val result = service.postcodeSearch("INVALID", None).futureValue

        result mustEqual Left(apiError)
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("INVALID", None)))(any())
      }

      "must return API error when initial search with property filter returns API error" in {

        val apiError = ApiError.BadRequestError
        when(
          mockAddressLookupConnector
            .searchByPostcode(eqTo(SearchByPostcodeRequest("INVALID", Some("Flat 1"))))(any())
        )
          .thenReturn(Future.successful(Left(apiError)))

        val result = service.postcodeSearch("INVALID", Some("Flat 1")).futureValue

        result mustEqual Left(apiError)
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("INVALID", Some("Flat 1"))))(any())
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(any())(any())
      }

      "must return API error when initial search with property filter returns empty and fallback returns API error" in {

        val apiError = ApiError.BadRequestError
        when(
          mockAddressLookupConnector
            .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", Some("Flat 555"))))(any())
        )
          .thenReturn(Future.successful(Right(Seq.empty)))
        when(mockAddressLookupConnector.searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any()))
          .thenReturn(Future.successful(Left(apiError)))

        val result = service.postcodeSearch("TE1 1ST", Some("Flat 555")).futureValue

        result mustEqual Left(apiError)
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", Some("Flat 555"))))(any())
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any())
      }

    }
  }
}
