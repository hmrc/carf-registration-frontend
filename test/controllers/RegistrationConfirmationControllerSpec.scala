package controllers

import base.SpecBase
import models.{SubscriptionId, UniqueTaxpayerReference, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.organisation.{FirstContactEmailPage, OrganisationSecondContactEmailPage}
import pages.{IndexPage, SubmissionSucceededPage, SubscriptionIdPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.EmailService
import views.html.RegistrationConfirmationView
import org.scalatest.OptionValues._

import scala.concurrent.Future
import scala.util.Success

class RegistrationConfirmationControllerSpec extends SpecBase with MockitoSugar {

  val mockSessionRepository: SessionRepository = mock[SessionRepository]
  val mockEmailService: EmailService = mock[EmailService]
  val mockView: RegistrationConfirmationView = mock[RegistrationConfirmationView]

  val subscriptionId = SubscriptionId("sub-123")
  val primaryEmail = "primary@email.com"
  val secondaryEmail = "secondary@email.com"
  val idNumber = "1234567890"
  val confirmationHtml = play.twirl.api.Html("<h1>Success!</h1>")

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionRepository, mockEmailService, mockView)
  }

  "RegistrationConfirmationController" - {

    "must render confirmation view and send email when all required data is present" in {
      val userAnswers = emptyUserAnswers
        .set(SubscriptionIdPage, subscriptionId).success.value
        .set(FirstContactEmailPage, primaryEmail).success.value
        .set(OrganisationSecondContactEmailPage, secondaryEmail).success.value
        .set(IndexPage, UniqueTaxpayerReference(idNumber)).success.value

      when(mockEmailService.sendRegistrationConfirmation(any(), any(), any()))
        .thenReturn(Future.successful(()))

      when(mockSessionRepository.set(any()))
        .thenReturn(Future.successful(true))

      when(mockView.apply(any(), any(), any(), any())(any(), any()))
        .thenReturn(confirmationHtml)

      val application = applicationBuilder(Some(userAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[EmailService].toInstance(mockEmailService),
          bind[RegistrationConfirmationView].toInstance(mockView)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.RegistrationConfirmationController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include ("Success!")
        verify(mockEmailService).sendRegistrationConfirmation(any(), any(), any())
        verify(mockSessionRepository).set(any())
      }
    }

    "must redirect to Journey Recovery if required data is missing" in {
      val userAnswers = emptyUserAnswers // Missing SubscriptionIdPage, etc.

      val application = applicationBuilder(Some(userAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[EmailService].toInstance(mockEmailService),
          bind[RegistrationConfirmationView].toInstance(mockView)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.RegistrationConfirmationController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery if persistence fails" in {
      val userAnswers = emptyUserAnswers
        .set(SubscriptionIdPage, subscriptionId).success.value
        .set(FirstContactEmailPage, primaryEmail).success.value

      when(mockEmailService.sendRegistrationConfirmation(any(), any(), any()))
        .thenReturn(Future.successful(()))

      when(mockSessionRepository.set(any()))
        .thenReturn(Future.failed(new RuntimeException("DB error")))

      val application = applicationBuilder(Some(userAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[EmailService].toInstance(mockEmailService),
          bind[RegistrationConfirmationView].toInstance(mockView)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.RegistrationConfirmationController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
