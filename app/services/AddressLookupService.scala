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
import models.error.{ApiError, CarfError}
import models.requests.SearchByPostcodeRequest
import models.responses.AddressResponse
import uk.gov.hmrc.http.HeaderCarrier
import cats.data.EitherT
import cats.syntax.all.*
import models.AddressUk

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddressLookupService @Inject() (addressLookupConnector: AddressLookupConnector) {

  def postcodeSearch(postcode: String, propertyNameOrNumber: Option[String])(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier
  ): Future[Either[CarfError, (Seq[AddressUk], Boolean)]] = {
    val initialRequest = SearchByPostcodeRequest(postcode = postcode, filter = propertyNameOrNumber)
    {
      for {
        addressLookupResponse: Seq[AddressResponse]                    <- addressLookupConnector.searchByPostcode(initialRequest)
        addressLookupCombinedResponse: (Seq[AddressResponse], Boolean) <-
          if (addressLookupResponse.nonEmpty || propertyNameOrNumber.isEmpty) {
            EitherT.right[ApiError](Future.successful((addressLookupResponse, false)))
          } else {
            for {
              address <- addressLookupConnector.searchByPostcode(
                           initialRequest.copy(filter = None)
                         )
            } yield (address, true)
          }
        (lookupResponse, additionalCall)                                = addressLookupCombinedResponse
        addressDomain: Seq[AddressUk]                                  <-
          EitherT.fromEither[Future](lookupResponse.traverse(AddressResponse.toDomainAddressUk))
      } yield (addressDomain, additionalCall)
    }.value
  }
}
