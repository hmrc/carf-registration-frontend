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

package controllers.actions

import base.SpecBase
import models.requests.OptionalDataRequest
import pages.SubmissionSucceededPage
import play.api.mvc.{AnyContentAsEmpty, BodyParsers, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status, SEE_OTHER}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation

import scala.concurrent.Future

class SubmissionLockActionSpec extends SpecBase {

  class TestableSubmissionLockAction(bodyParsers: BodyParsers.Default) extends SubmissionLockAction(bodyParsers) {
    def testFilter[A](request: OptionalDataRequest[A]): Future[Option[Result]] = filter(request)
  }

  val testSubmissionLockAction = new TestableSubmissionLockAction(app.injector.instanceOf[BodyParsers.Default])

  val optionalDataRequestWithSubmissionSucceeded: OptionalDataRequest[AnyContentAsEmpty.type] =
    OptionalDataRequest(
      request = FakeRequest(),
      userId = testInternalId,
      affinityGroup = Organisation,
      userAnswers = Some(emptyUserAnswers.withPage(SubmissionSucceededPage, true))
    )

  val optionalDataRequestWithoutSubmissionSucceeded: OptionalDataRequest[AnyContentAsEmpty.type] =
    OptionalDataRequest(
      request = FakeRequest(),
      userId = testInternalId,
      affinityGroup = Organisation,
      userAnswers = Some(emptyUserAnswers)
    )

  val optionalDataRequestWithNoUserAnswers: OptionalDataRequest[AnyContentAsEmpty.type] =
    OptionalDataRequest(
      request = FakeRequest(),
      userId = testInternalId,
      affinityGroup = Organisation,
      userAnswers = None
    )

  "SubmissionLockAction" - {

    "must return None when the user has not successfully submitted" in {
      val result: Option[Result] =
        testSubmissionLockAction.testFilter(optionalDataRequestWithoutSubmissionSucceeded).futureValue

      result mustBe None
    }

    "must return None when the user has no UserAnswers" in {
      val result: Option[Result] =
        testSubmissionLockAction.testFilter(optionalDataRequestWithNoUserAnswers).futureValue

      result mustBe None
    }

    "must redirect to PageUnavailable when the user has successfully submitted" in {
      val result: Option[Result] =
        testSubmissionLockAction.testFilter(optionalDataRequestWithSubmissionSucceeded).futureValue

      result.isDefined                                mustBe true
      status(Future.successful(result.get))           mustBe SEE_OTHER
      redirectLocation(Future.successful(result.get)) mustBe Some(
        controllers.routes.PageUnavailableController.onPageLoad().url
      )
    }
  }
}
