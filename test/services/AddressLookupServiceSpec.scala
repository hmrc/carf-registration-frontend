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
import cats.data.EitherT
import connectors.AddressLookupConnector
import generators.Generators
import models.error.{ApiError, CarfError, ConversionError}
import models.requests.SearchByPostcodeRequest
import models.responses.AddressResponse
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class AddressLookupServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with Generators {

  val mockAddressLookupConnector: AddressLookupConnector = mock[AddressLookupConnector]

  val service: AddressLookupService = new AddressLookupService(mockAddressLookupConnector)

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
          .thenReturn(EitherT.rightT[Future, CarfError](Seq(oneAddressResponse)))

        val Right(result, retry) = service.postcodeSearch("TE1 1ST", Some("Flat 1")).futureValue

        result mustBe Right(Seq(testAddressUk))
        retry  mustEqual false
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", Some("Flat 1"))))(any())
      }

      "must return addresses when initial search without property filter succeeds" in {
        when(mockAddressLookupConnector.searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any()))
          .thenReturn(EitherT.rightT[Future, CarfError](multipleAddressResponses))

        val Right(result, retry) = service.postcodeSearch("TE1 1ST", None).futureValue

        result mustBe Right(Seq(testAddressUk, testAddressUk, testAddressUk))
        result mustEqual sampleAddresses
        retry  mustEqual false
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any())
      }

      "must return addresses when retrying without filter after initial search returns empty results" in {
        when(
          mockAddressLookupConnector
            .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", Some("Flat 99"))))(any())
        )
          .thenReturn(EitherT.rightT[Future, CarfError](Seq.empty))
        when(mockAddressLookupConnector.searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any()))
          .thenReturn(EitherT.rightT[Future, CarfError](multipleAddressResponses))

        val Right(result, retry) = service.postcodeSearch("TE1 1ST", Some("Flat 99")).futureValue

        result mustBe Right(Seq(testAddressUk, testAddressUk, testAddressUk))
        result mustEqual sampleAddresses
        retry  mustEqual true
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
          .thenReturn(EitherT.rightT[Future, CarfError](Seq.empty))
        when(mockAddressLookupConnector.searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any()))
          .thenReturn(EitherT.rightT[Future, CarfError](Seq.empty))

        val Right(result, retry) = service.postcodeSearch("TE1 1ST", Some("Flat 99")).futureValue

        result mustEqual Seq.empty
        retry  mustEqual true
        result mustBe Right(Seq.empty)
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", Some("Flat 99"))))(any())
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any())
      }

      "must return empty sequence when initial search without property filter returns empty results" in {
        when(mockAddressLookupConnector.searchByPostcode(eqTo(SearchByPostcodeRequest("XX1 1XX", None)))(any()))
          .thenReturn(EitherT.rightT[Future, CarfError](Seq.empty))

        val Right(result, retry) = service.postcodeSearch("XX1 1XX", None).futureValue

        result mustEqual Seq.empty
        retry  mustEqual false

        result mustBe Right(Seq.empty)
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("XX1 1XX", None)))(any())
      }

      "must return API error when initial search returns API error" in {
        when(mockAddressLookupConnector.searchByPostcode(eqTo(SearchByPostcodeRequest("INVALID", None)))(any()))
          .thenReturn(EitherT.leftT[Future, Seq[AddressResponse]](ApiError.BadRequestError))

        val Left(result) = service.postcodeSearch("INVALID", None).futureValue

        result mustBe Left(ApiError.BadRequestError)
        result mustEqual apiError
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("INVALID", None)))(any())
      }

      "must return API error when initial search with property filter returns API error" in {

        when(
          mockAddressLookupConnector
            .searchByPostcode(eqTo(SearchByPostcodeRequest("INVALID", Some("Flat 1"))))(any())
        )
          .thenReturn(EitherT.leftT[Future, Seq[AddressResponse]](ApiError.BadRequestError))

        val Left(result) = service.postcodeSearch("INVALID", Some("Flat 1")).futureValue

        result mustBe Left(ApiError.BadRequestError)
        result mustEqual apiError
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("INVALID", Some("Flat 1"))))(any())
      }

      "must return API error when initial search with property filter returns empty and fallback returns API error" in {
        when(
          mockAddressLookupConnector
            .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", Some("Flat 555"))))(any())
        )
          .thenReturn(EitherT.rightT[Future, ApiError](Seq.empty))
        when(mockAddressLookupConnector.searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any()))
          .thenReturn(EitherT.leftT[Future, Seq[AddressResponse]](ApiError.BadRequestError))

        val Left(result) = service.postcodeSearch("TE1 1ST", Some("Flat 555")).futureValue

        result mustBe Left(ApiError.BadRequestError)
        result mustEqual apiError
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", Some("Flat 555"))))(any())
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any())
      }

      "must return a ConversionError when the address response can not be transformed into an Address UK model" in {
        when(mockAddressLookupConnector.searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any()))
          .thenReturn(
            EitherT.rightT[Future, CarfError](
              Seq(oneAddressResponse.copy(address = oneAddressResponse.address.copy(lines = List.empty)))
            )
          )

        val result = service.postcodeSearch("TE1 1ST", None).futureValue

        result mustBe Left(ConversionError)
        verify(mockAddressLookupConnector, times(1))
          .searchByPostcode(eqTo(SearchByPostcodeRequest("TE1 1ST", None)))(any())
      }
    }
  }
}
