package controllers.actions

import base.SpecBase
import config.FrontendAppConfig
import models.requests.IdentifierRequest
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.OK
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}

import scala.concurrent.Future

class CheckEnrolledToServiceActionSpec extends SpecBase {

  val mockConfig = mock[FrontendAppConfig]

  val testContent = "Test Content"

  val enrolment      = "MTD-AD-ORG"
  val carfKey        = "HMRC-CARF-ORG"
  val state          = "Activated"
  val identifierName = "CARFID"

  val enrolments      = Set(Enrolment(carfKey, Seq(EnrolmentIdentifier(identifierName, "456")), state))
  val emptyEnrolments = Set.empty[Enrolment]

  val identifierRequestWithCarfEnrolment: IdentifierRequest[AnyContentAsEmpty.type] =
    IdentifierRequest(
      request = FakeRequest(),
      userId = testInternalId,
      affinityGroup = Organisation,
      enrolments = enrolments
    )

  val identifierRequestWithoutCarfEnrolment: IdentifierRequest[AnyContentAsEmpty.type] =
    IdentifierRequest(request = FakeRequest(), userId = testInternalId, affinityGroup = Organisation)

  val testCheckEnrolledToServiceAction = new CheckEnrolledToServiceAction(mockConfig)

  "CheckEnrolledToServiceAction" - {

    "must return None when the user is not already enrolled to CARF" in {
      when(mockConfig.enrolmentKey).thenReturn(carfKey)

      val result: Option[Result] =
        testCheckEnrolledToServiceAction.filter(identifierRequestWithoutCarfEnrolment).futureValue

      result mustBe None
    }

    "must return a page saying that the user is already enrolled to CARF" in {
      when(mockConfig.enrolmentKey).thenReturn(carfKey)

      val result: Future[Result] =
        Future.successful(testCheckEnrolledToServiceAction.filter(identifierRequestWithCarfEnrolment).futureValue.get)

      status(result) mustBe OK
      contentAsString(
        result
      )              mustBe "User is enrolled. We need to bring them to the service. TODO: Redirect to management FE URL"
    }
  }
}
