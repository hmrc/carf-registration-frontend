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

package controllers.actions

import base.SpecBase
import controllers.routes
import models.requests.{DataRequest, IdentifierRequest, OptionalDataRequest}
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.LOCATION
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import scala.concurrent.Future

class DataRequiredActionSpec extends SpecBase {

  private def fakeRequest = FakeRequest("", "")

  class Harness extends DataRequiredActionImpl {
    def actionRefine[A](request: OptionalDataRequest[A]): Future[Either[Result, DataRequest[A]]] = refine(request)
  }

  val identifierRequest: IdentifierRequest[AnyContentAsEmpty.type] =
    IdentifierRequest(FakeRequest(), testInternalId, Individual)

  "Data Required Action" - {

    "must redirect to the Journey Recovery when User Answers is None" in {

      val harness = new Harness

      val result =
        harness
          .actionRefine(
            OptionalDataRequest(identifierRequest, testInternalId, Individual, None)
          )
          .futureValue
          .left
          .getOrElse(
            fail()
          )
          .header

      result.status                mustEqual SEE_OTHER
      result.headers.get(LOCATION) mustEqual Some(routes.JourneyRecoveryController.onPageLoad().url)
    }

    "must return a DataRequest with the correct values when User Answeres exists" in {

      val harness = new Harness

      val optionalDataRequest =
        OptionalDataRequest(identifierRequest, testInternalId, Individual, Some(emptyUserAnswers))

      val result =
        harness
          .actionRefine(optionalDataRequest)
          .futureValue

      result.isRight mustBe true
      result.map { dataRequest =>
        dataRequest.request       mustEqual identifierRequest
        dataRequest.userAnswers   mustEqual emptyUserAnswers
        dataRequest.userId        mustEqual testInternalId
        dataRequest.affinityGroup mustEqual Individual
      }
    }
  }

}
