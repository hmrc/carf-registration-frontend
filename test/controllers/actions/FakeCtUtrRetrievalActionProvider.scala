package controllers.actions

import models.UniqueTaxpayerReference
import models.requests.IdentifierRequest
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class FakeCtUtrRetrievalActionProvider(
                                        utr: Option[UniqueTaxpayerReference] = None
                                      ) extends CtUtrRetrievalAction {

  def apply(): ActionFunction[IdentifierRequest, IdentifierRequest] =
    new FakeCtUtrRetrievalAction(utr)

}

class FakeCtUtrRetrievalAction(
                                utr: Option[UniqueTaxpayerReference] = None
                              ) extends ActionFunction[IdentifierRequest, IdentifierRequest] {

  override def invokeBlock[A](request: IdentifierRequest[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] =
    block(request.copy(utr = utr))

  implicit override protected val executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

}

