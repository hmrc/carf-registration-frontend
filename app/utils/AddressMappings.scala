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

package utils

import models.AddressUk
import models.countries.CountryUk
import models.error.{CarfError, ConversionError}
import models.responses.AddressResponse

object AddressMappings {

  extension (addressResponse: AddressResponse)
    def toDomain: Either[CarfError, AddressUk] = {
      val address = addressResponse.address
      address.lines match {
        case head :: next =>
          Right(
            AddressUk(
              addressLine1 = head,
              addressLine2 = next.headOption,
              addressLine3 = next.lift(1),
              townOrCity = address.town,
              postCode = address.postcode,
              countryUk = CountryUk(code = address.country.code, name = address.country.name)
            )
          )
        case Nil          => Left(ConversionError)
      }
    }
}
