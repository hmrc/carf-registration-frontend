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

package connectors
import config.FrontendAppConfig
import models.error.ApiError.InternalServerError
import models.error.CarfError
import org.apache.pekko.actor.{ActorSystem, Scheduler}
import org.apache.pekko.pattern.retry
import types.ResultT
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggerUtil.logError

import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}

trait RetryMechanism {
  val config: FrontendAppConfig
  val system: ActorSystem

  implicit val ex: ExecutionContext = system.dispatcher
  implicit val scheduler: Scheduler = system.scheduler

  /** apiRetryAttempts //+1 from total is equal to total attempts. e.g. apiRetryAttempts = 2, retries twice so total
    * attempts is equal to 3
    */
  protected def retryCall[T](call: => ResultT[T])(implicit hc: HeaderCarrier): ResultT[T] =
    ResultT.fromFuture(
      retry(
        () => call.value,
        config.apiRetryAttempts,
        config.apiRetryDelayInSeconds.seconds
      ).recoverWith { e =>
        logError(s"Retry Failed returning final outcome with message: \n${e.getMessage}")
        Future.successful(Left(InternalServerError))
      }
    )
}
