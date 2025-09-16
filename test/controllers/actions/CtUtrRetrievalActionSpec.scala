package controllers.actions

import base.SpecBase
import config.FrontendAppConfig
import models.requests.IdentifierRequest
import org.mockito.Mockito.when
import play.api.http.Status.OK
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}

import scala.concurrent.Future

class CtUtrRetrievalActionSpec extends SpecBase {

  private def fakeRequest = FakeRequest("", "")

  val mockConfig = mock[FrontendAppConfig]

  val testContent     = "Test Content "
  val enrolment       = "IR-CT"
  val utrKey          = "UTR"
  val state           = "Activated"
  val enrolments      = Set(Enrolment(enrolment, Seq(EnrolmentIdentifier(utrKey, "123")), state))
  val emptyEnrolments = Set.empty[Enrolment]

  val testCtUtrRetrievalActionProvider = new CtUtrRetrievalActionProvider(mockConfig)

  val identifierRequestOrganisationWithEnrolment: IdentifierRequest[AnyContentAsEmpty.type] =
    IdentifierRequest(
      request = FakeRequest(),
      userId = testInternalId,
      affinityGroup = Organisation,
      enrolments = enrolments
    )

  val identifierRequestOrganisationNoEnrolment: IdentifierRequest[AnyContentAsEmpty.type] =
    IdentifierRequest(request = FakeRequest(), userId = testInternalId, affinityGroup = Organisation)

  val identifierRequestIndividualWithEnrolment: IdentifierRequest[AnyContentAsEmpty.type] =
    IdentifierRequest(request = FakeRequest(), userId = testInternalId, affinityGroup = Individual)

  val testAction: IdentifierRequest[_] => Future[Result] = { request =>
    Future(Ok(testContent + request.utr.map(_.uniqueTaxPayerReference).getOrElse("No UTR")))
  }

  "CtUtrRetrievalActionProvider" - {

    "must add utr to the request if it has been retrieved as an enrolment and affinity group is Organisation" in {
      when(mockConfig.ctEnrolmentKey).thenReturn(enrolment)

      val result: Future[Result] =
        testCtUtrRetrievalActionProvider.invokeBlock(identifierRequestOrganisationWithEnrolment, testAction)

      status(result)          mustBe OK
      contentAsString(result) mustBe "Test Content 123"
    }

    "must not add utr to the request if it has been retrieved as an enrolment but affinity group is Individual" in {
      when(mockConfig.ctEnrolmentKey).thenReturn(enrolment)

      val result: Future[Result] =
        testCtUtrRetrievalActionProvider.invokeBlock(identifierRequestIndividualWithEnrolment, testAction)

      status(result)          mustBe OK
      contentAsString(result) mustBe "Test Content No UTR"
    }

    "must not add utr to the request if it has not been retrieved as an enrolment but affinity group is Organisation" in {
      when(mockConfig.ctEnrolmentKey).thenReturn(enrolment)

      val result: Future[Result] =
        testCtUtrRetrievalActionProvider.invokeBlock(identifierRequestOrganisationNoEnrolment, testAction)

      status(result)          mustBe OK
      contentAsString(result) mustBe "Test Content No UTR"
    }
  }

}
