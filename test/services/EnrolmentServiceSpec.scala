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
import connectors.EnrolmentConnector
import models.SubscriptionId
import models.error.ApiError.InternalServerError
import models.requests.{EnrolmentRequest, Identifier, Verifier}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import types.ResultT

class EnrolmentServiceSpec extends SpecBase {

  val mockConnector    = mock[EnrolmentConnector]
  val enrolmentService = new EnrolmentService(mockConnector)

  "EnrolmentService" - {
    "createEnrolment" - {
      "should successfully enrol" in {

        val carfId   = "testCarfI"
        val postcode = "NW6 8RT"

        val enrolmentRequest = EnrolmentRequest(
          Seq(Identifier("CARFID", carfId)),
          Seq(
            Verifier("PostCode", postcode),
            Verifier("IsAbroad", "N")
          )
        )

        when(mockConnector.createEnrolment(ArgumentMatchers.eq(enrolmentRequest))(any()))
          .thenReturn(ResultT.fromValue(()))

        val result = enrolmentService.enrol(SubscriptionId(carfId), Some(postcode), false).value.futureValue

        result.isRight mustBe true
      }

      "should return error when connector fails" in {
        when(mockConnector.createEnrolment(any())(any())).thenReturn(ResultT.fromError(InternalServerError))

        val result = enrolmentService.enrol(SubscriptionId("testCarfI"), Some("NW6 8RT"), false).value.futureValue

        result.isLeft mustBe true
      }
    }
  }
}
