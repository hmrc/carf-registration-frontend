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

import models.{Address, Business}
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class BusinessService @Inject() () {

  def getBusinessByUtr(utr: String): Future[Option[Business]] =
    Future.successful {
      // temp implementation as auto-matching not yet implemented
      if (utr.startsWith("1")) {
        // UK business
        Some(
          Business(
            name = "Agent ABC Ltd",
            address = Address(
              line1 = "2 High Street",
              line2 = "Birmingham",
              postcode = "B23 2AZ",
              country = None
            ),
            isUkBased = true
          )
        )
      } else if (utr.startsWith("2")) {
        // Non-UK business
        Some(
          Business(
            name = "International Corp Ltd",
            address = Address(
              line1 = "3 Apple Street",
              line2 = "New York",
              postcode = "11722",
              country = Some("United States")
            ),
            isUkBased = false
          )
        )
      } else {
        // Business not found
        None
      }
    }
}
