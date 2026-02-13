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

import play.api.Logging
import models.requests.OptionalDataRequest
import play.api.mvc._
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import pages.SubmissionSucceededPage

class SubmissionLockAction @Inject() (val parser: BodyParsers.Default)(implicit val ec: ExecutionContext)
    extends ActionFilter[OptionalDataRequest]
    with Logging {

  override protected def executionContext: ExecutionContext = ec

  override protected def filter[A](request: OptionalDataRequest[A]): Future[Option[Result]] = {

    val submitted = request.userAnswers.exists(_.get(SubmissionSucceededPage).contains(true))

    if (submitted) {

      logger.info(
        s"[SubmissionLockAction] Blocking request after submission. " +
          s"affinityGroup=${request.affinityGroup}, path=${request.uri}"
      )

      Future.successful(
        Some(
          Results.Redirect(controllers.routes.PageUnavailableController.onPageLoad())
        )
      )

    } else {
      Future.successful(None)
    }
  }
}

class SubmissionLockActionProvider @Inject() (parsers: BodyParsers.Default)(implicit ec: ExecutionContext) {

  def apply(): SubmissionLockAction =
    new SubmissionLockAction(parsers)

}
