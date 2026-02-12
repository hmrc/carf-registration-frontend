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
import play.api.test.Helpers.{redirectLocation, status, SEE_OTHER}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import scala.concurrent.Future

class SubmissionLockActionSpec extends SpecBase {

  private val parsers = app.injector.instanceOf[BodyParsers.Default]
  private val action  = new SubmissionLockAction(parsers)

  private val requestWithSubmissionSucceeded =
    OptionalDataRequest(
      request = FakeRequest(),
      userId = testInternalId,
      affinityGroup = Organisation,
      userAnswers = Some(emptyUserAnswers.withPage(SubmissionSucceededPage, true))
    )

  private val requestWithoutSubmissionSucceeded =
    OptionalDataRequest(
      request = FakeRequest(),
      userId = testInternalId,
      affinityGroup = Organisation,
      userAnswers = Some(emptyUserAnswers)
    )

  private val requestWithNoUserAnswers =
    OptionalDataRequest(
      request = FakeRequest(),
      userId = testInternalId,
      affinityGroup = Organisation,
      userAnswers = None
    )

  "SubmissionLockAction" - {

    "return None when the user has not successfully submitted" in {
      val result = action.filter(requestWithoutSubmissionSucceeded).futureValue
      result mustBe None
    }

    "return None when the user has no UserAnswers" in {
      val result = action.filter(requestWithNoUserAnswers).futureValue
      result mustBe None
    }

    "redirect to PageUnavailable when the user has successfully submitted" in {
      val result = action.filter(requestWithSubmissionSucceeded).futureValue
      result mustBe defined

      status(Future.successful(result.value))           mustBe SEE_OTHER
      redirectLocation(Future.successful(result.value)) mustBe Some(
        controllers.routes.PageUnavailableController.onPageLoad().url
      )
    }
  }
}
