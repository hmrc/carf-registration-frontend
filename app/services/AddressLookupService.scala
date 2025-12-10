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

package services

import connectors.{AddressLookupConnector, RegistrationConnector}
import models.error.ApiError
import models.requests.SearchByPostcodeRequest
import models.responses.AddressResponse
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddressLookupService @Inject() (connector: AddressLookupConnector)(implicit ec: ExecutionContext)
    extends Logging {

//  def searchByPostcode(postcode: String, filter: Option[String])(implicit
//      hc: HeaderCarrier
//  ): Future[Either[ApiError, AddressResponse]] = {
//    val normalised = postcode.toUpperCase.replaceAll("\\s+", "")
//
//    val postcodeRegex = """^([A-Z]{1,2})([0-9][0-9A-Z]?)([0-9])([A-Z]{2})$""".r
//
//    normalised match {
//      case postcodeRegex(area, district, sector, unit) =>
//        val request: SearchByPostcodeRequest =
//          SearchByPostcodeRequest(postcode = Postcode(area, district, sector, unit), filter = filter)
//        connector.searchByPostcode(request)
//
//      case _ => Future.successful(Left(ApiError.BadRequestError)) // invalid postcode
//    }
//
//  }

}
