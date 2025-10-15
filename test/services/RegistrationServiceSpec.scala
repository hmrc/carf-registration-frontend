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

import models.{Business, UniqueTaxpayerReference}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationServiceSpec extends AnyWordSpec with Matchers with ScalaFutures {

  val businessService = new RegistrationService()

  "BusinessService" should {

    "return UK business for UTR starting with '1'" in {
      val result = businessService.getBusinessByUtr("1234567890")

      val business = result.futureValue
      business                          shouldBe defined
      business.get.name                 shouldBe "Agent ABC Ltd"
      business.get.isUkBased            shouldBe true
      business.get.address.addressLine1 shouldBe "2 High Street"
      business.get.address.addressLine2 shouldBe Some("Birmingham")
      business.get.address.postalCode   shouldBe Some("B23 2AZ")
      business.get.address.countryCode  shouldBe "GB"
    }

    "return Non-UK business for UTR starting with '2'" in {
      val result = businessService.getBusinessByUtr("2987654321")

      val business = result.futureValue
      business                          shouldBe defined
      business.get.name                 shouldBe "International Ltd"
      business.get.isUkBased            shouldBe false
      business.get.address.addressLine1 shouldBe "3 Apple Street"
      business.get.address.addressLine2 shouldBe Some("New York")
      business.get.address.postalCode   shouldBe Some("11722")
      business.get.address.countryCode  shouldBe "US"
    }

    "return a business when UTR and businessName has been provided" in {
      val result =
        businessService.getBusinessName(
          uniqueTaxpayerReference = UniqueTaxpayerReference("1234567890"),
          businessName = "Agent ABC Ltd"
        )

      val business = result.futureValue

      business                          shouldBe defined
      business.get.name                 shouldBe "Agent ABC Ltd"
      business.get.isUkBased            shouldBe true
      business.get.address.addressLine1 shouldBe "2 High Street"
      business.get.address.addressLine2 shouldBe Some("Birmingham")
    }
  }
}
