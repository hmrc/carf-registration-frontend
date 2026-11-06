/*
 * Copyright 2024 HM Revenue & Customs
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

package config

import base.SpecBase
import models.SubscriptionId
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder

class ClaimEnrolmentInformationProviderSpec extends SpecBase {

  "claimEnrolmentInformation" - {
    "must be empty when there are no details in config" in {
      val keysToRemove = Seq(
        "claimEnrolmentInformation.1.safeId",
        "claimEnrolmentInformation.1.carfId",
        "claimEnrolmentInformation.1.isAbroad",
        "claimEnrolmentInformation.2.safeId",
        "claimEnrolmentInformation.2.carfId",
        "claimEnrolmentInformation.2.isAbroad"
      )

      val app = new GuiceApplicationBuilder()
        .loadConfig { env =>
          val config = keysToRemove.foldLeft(Configuration.load(env).underlying)(_.withoutPath(_))
          Configuration(config)
        }
        .build()

      val provider = app.injector.instanceOf[ClaimEnrolmentInformationProvider]

      provider.claimEnrolmentInformation mustBe Some(List.empty)
    }
  }

  "must return Subscription Identifiers when the encrypted values are present in config" in {
    val app      = new GuiceApplicationBuilder()
      .configure("claimEnrolmentInformation.0.safeId" -> "safeId-0")
      .configure("claimEnrolmentInformation.0.carfId" -> "carfId-0")
      .configure("claimEnrolmentInformation.0.isAbroad" -> true)
      .configure("claimEnrolmentInformation.1.safeId" -> "safeId-1")
      .configure("claimEnrolmentInformation.1.carfId" -> "carfId-1")
      .configure("claimEnrolmentInformation.1.isAbroad" -> false)
      .configure("claimEnrolmentInformation.2.safeId" -> "safeId-2")
      .configure("claimEnrolmentInformation.2.carfId" -> "carfId-2")
      .configure("claimEnrolmentInformation.2.isAbroad" -> true)
      .build()
    val provider = app.injector.instanceOf[ClaimEnrolmentInformationProvider]

    provider.claimEnrolmentInformation.value mustEqual Seq(
      ClaimEnrolmentInformation("safeId-0", SubscriptionId("carfId-0"), true),
      ClaimEnrolmentInformation("safeId-1", SubscriptionId("carfId-1"), false),
      ClaimEnrolmentInformation("safeId-2", SubscriptionId("carfId-2"), true)
    )
  }
}
