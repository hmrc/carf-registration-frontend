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

import models.requests.PasscodeRequest
import play.api.mvc.*

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FakePasscodeAction @Inject() (bodyParsers: PlayBodyParsers)
    extends PasscodeAction
    with ActionBuilder[PasscodeRequest, AnyContent]
    with ActionFunction[Request, PasscodeRequest] {

  override def invokeBlock[A](request: Request[A], block: PasscodeRequest[A] => Future[Result]): Future[Result] =
    block(PasscodeRequest(request, "id"))

  override def parser: BodyParser[AnyContent] =
    bodyParsers.default

  override protected def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  override def apply(
      passcode: String = "TestCode123"
  ): ActionBuilder[PasscodeRequest, AnyContent] with ActionFunction[Request, PasscodeRequest] = this

}
