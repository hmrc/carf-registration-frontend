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

import models.Business
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.concurrent.ScalaFutures

class BusinessServiceSpec extends AnyWordSpec with Matchers with ScalaFutures {

  val businessService = new BusinessService()

  "BusinessService" should {

    "return UK business for UTR starting with '1'" in {
      val result = businessService.getBusinessByUtr("1234567890")

      val business = result.futureValue
      business                      shouldBe defined
      business.get.name             shouldBe "Agent ABC Ltd"
      business.get.isUkBased        shouldBe true
      business.get.address.line1    shouldBe "2 High Street"
      business.get.address.line2    shouldBe "Birmingham"
      business.get.address.postcode shouldBe "B23 2AZ"
      business.get.address.country  shouldBe None
    }

    "return Non-UK business for UTR starting with '2'" in {
      val result = businessService.getBusinessByUtr("2987654321")

      val business = result.futureValue
      business                      shouldBe defined
      business.get.name             shouldBe "International Corp Ltd"
      business.get.isUkBased        shouldBe false
      business.get.address.line1    shouldBe "3 Apple Street"
      business.get.address.line2    shouldBe "New York"
      business.get.address.postcode shouldBe "11722"
      business.get.address.country  shouldBe Some("United States")
    }

    "return None for UTR starting with any other digit" in {
      val result = businessService.getBusinessByUtr("3123456789")

      result.futureValue shouldBe None
    }
  }
}
