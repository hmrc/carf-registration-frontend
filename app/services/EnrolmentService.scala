package services

import config.Constants.CARFID
import connectors.EnrolmentConnector
import models.requests.{EnrolmentRequest, Identifier, Verifier}
import play.api.Logging
import types.ResultT
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class EnrolmentService @Inject() (enrolmentConnector: EnrolmentConnector) extends Logging {
  
  def enrol(carfId: String, postcode: String, isAbroad: Boolean)(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[Unit] = {
    val enrolmentRequest = EnrolmentRequest(
      Seq(Identifier(CARFID, carfId)),
      Seq(
        Verifier("PostCode", postcode),
        Verifier("isAbroad", if(isAbroad) "Y" else "N")
      )
    )
    
    enrolmentConnector.createEnrolment(enrolmentRequest).leftMap { error =>
      logger.error(s"Failed to create subscription: $error")
      error
    }
  }
}
