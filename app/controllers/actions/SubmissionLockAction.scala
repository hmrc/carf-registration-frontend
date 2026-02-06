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

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import models.requests.OptionalDataRequest
import pages.SubmissionSucceededPage

@Singleton
class SubmissionLockAction @Inject() (
    val parser: BodyParsers.Default
)(implicit ec: ExecutionContext)
    extends ActionFilter[OptionalDataRequest] {

  override protected def executionContext: ExecutionContext = ec

  override protected def filter[A](request: OptionalDataRequest[A]): Future[Option[Result]] = {
    val submitted = request.userAnswers match {
      case Some(ua) => ua.get(SubmissionSucceededPage).contains(true)
      case None     => false
    }

    if (submitted) {
      Future.successful(
        Some(Results.Redirect(controllers.routes.PageUnavailableController.onPageLoad()))
      )
    } else {
      Future.successful(None)
    }
  }
}
