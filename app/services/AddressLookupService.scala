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

import connectors.AddressLookupConnector
import models.error.ApiError
import models.requests.SearchByPostcodeRequest
import models.responses.AddressResponse
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddressLookupService @Inject() (addressLookupConnector: AddressLookupConnector) {

  def postcodeSearch(postcode: String, propertyNameOrNumber: Option[String])(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier
  ): Future[Either[ApiError, Seq[AddressResponse]]] = {
    val initialRequest = SearchByPostcodeRequest(postcode = postcode, filter = propertyNameOrNumber)

    addressLookupConnector.searchByPostcode(initialRequest).flatMap {
      case Left(error) =>
        Future.successful(Left(error))

      case Right(Nil) if propertyNameOrNumber.isDefined =>
        val fallbackRequest = SearchByPostcodeRequest(postcode = postcode, filter = None)
        addressLookupConnector.searchByPostcode(fallbackRequest)

      case Right(addresses) =>
        Future.successful(Right(addresses))
    }
  }
}
