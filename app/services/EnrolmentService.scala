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

import config.Constants.CARFID
import connectors.EnrolmentConnector
import models.SubscriptionId
import models.requests.{EnrolmentRequest, Identifier, Verifier}
import play.api.Logging
import types.ResultT
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class EnrolmentService @Inject() (enrolmentConnector: EnrolmentConnector) extends Logging {

  def enrol(carfId: SubscriptionId, postcodeMaybe: Option[String], isAbroad: Boolean)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext
  ): ResultT[Unit] = {

    val postCodeVerifierSeq = postcodeMaybe.map(postcode => Verifier("PostCode", postcode))

    val enrolmentRequest = EnrolmentRequest(
      Seq(Identifier(CARFID, carfId.value)),
      postCodeVerifierSeq.toSeq ++ Seq(Verifier("IsAbroad", if (isAbroad) "Y" else "N"))
    )

    enrolmentConnector.createEnrolment(enrolmentRequest).leftMap { error =>
      logger.error(s"Failed to create enrolment: $error")
      error
    }
  }
}
