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

§import base.SpecBase
import connectors.RegistrationConnector
import models.Business
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.concurrent.ScalaFutures

class RegistrationServiceSpec extends SpecBase {

  val mockConnector: RegistrationConnector = mock[RegistrationConnector]
  val businessService                      = new RegistrationService(mockConnector)

  "BusinessService" - {

    "return UK business for UTR starting with '1'" in {
      val result = businessService.getBusinessByUtr("1234567890")

      val business = result.futureValue
      business                          mustBe defined
      business.get.name                 mustBe "Agent ABC Ltd"
      business.get.isUkBased            mustBe true
      business.get.address.addressLine1 mustBe "2 High Street"
      business.get.address.addressLine2 mustBe Some("Birmingham")
      business.get.address.postalCode   mustBe Some("B23 2AZ")
      business.get.address.countryCode  mustBe "GB"
    }

    "return Non-UK business for UTR starting with '2'" in {
      val result = businessService.getBusinessByUtr("2987654321")

      val business = result.futureValue
      business                          mustBe defined
      business.get.name                 mustBe "International Ltd"
      business.get.isUkBased            mustBe false
      business.get.address.addressLine1 mustBe "3 Apple Street"
      business.get.address.addressLine2 mustBe Some("New York")
      business.get.address.postalCode   mustBe Some("11722")
      business.get.address.countryCode  mustBe "US"
    }

    "return None for UTR starting with any other digit" in {
      val result = businessService.getBusinessByUtr("3123456789")

      result.futureValue mustBe None
    }
  }
}
