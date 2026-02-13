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

package controllers

import base.SpecBase
import models.JourneyType._
import models.{UniqueTaxpayerReference, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.individual.{IndividualEmailPage, NiNumberPage}
import pages.organisation.{FirstContactEmailPage, OrganisationSecondContactEmailPage, UniqueTaxpayerReferenceInUserAnswers}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.{EmailService, SubscriptionService}
import views.html.RegistrationConfirmationView

import scala.concurrent.Future

class RegistrationConfirmationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockEmailService        = mock[EmailService]
  private val mockView                = mock[RegistrationConfirmationView]
  private val mockSubscriptionService = mock[SubscriptionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockEmailService, mockView, mockSubscriptionService)
  }

  "RegistrationConfirmationController" - {

    "onPageLoad" - {

      "when user is OrgWithUtr" - {

        "must return OK and render the view with correct data" in {

          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(OrgWithUtr))
            .withPage(FirstContactEmailPage, "org@test.com")
            .withPage(OrganisationSecondContactEmailPage, "org2@test.com")
            .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("1234567890"))

          when(mockEmailService.sendRegistrationConfirmation(any(), any(), any()))
            .thenReturn(Future.successful(()))
          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))
          when(mockView.apply(any(), any(), any(), any())(any(), any()))
            .thenReturn(HtmlFormat.raw("<p>Test view</p>"))

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[EmailService].toInstance(mockEmailService),
              bind[SubscriptionService].toInstance(mockSubscriptionService),
              bind[RegistrationConfirmationView].toInstance(mockView)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.RegistrationConfirmationController.onPageLoad().url)
            val result  = route(application, request).value

            status(result) mustEqual OK

            verify(mockEmailService).sendRegistrationConfirmation(
              eqTo(List("org@test.com", "org2@test.com")),
              eqTo("XXCAR0012345678"),
              eqTo(Some("1234567890"))
            )

            verify(mockSessionRepository).set(any())
          }
        }
      }

      "when user is IndWithNino" - {

        "must return OK and render the view with NINO" in {

          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithNino))
            .withPage(IndividualEmailPage, "individual@test.com")
            .withPage(NiNumberPage, "AB123456C")

          when(mockEmailService.sendRegistrationConfirmation(any(), any(), any()))
            .thenReturn(Future.successful(()))
          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))
          when(mockView.apply(any(), any(), any(), any())(any(), any()))
            .thenReturn(HtmlFormat.raw("<p>Test view</p>"))

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[EmailService].toInstance(mockEmailService),
              bind[SubscriptionService].toInstance(mockSubscriptionService),
              bind[RegistrationConfirmationView].toInstance(mockView)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.RegistrationConfirmationController.onPageLoad().url)
            val result  = route(application, request).value

            status(result) mustEqual OK

            verify(mockEmailService).sendRegistrationConfirmation(
              eqTo(List("individual@test.com")),
              eqTo("XXCAR0012345678"),
              eqTo(Some("AB123456C"))
            )
          }
        }
      }

      "when user is OrgWithoutId" - {

        "must return OK and render the view with no ID number" in {

          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(OrgWithoutId))
            .withPage(FirstContactEmailPage, "orgwithout@test.com")

          when(mockEmailService.sendRegistrationConfirmation(any(), any(), any()))
            .thenReturn(Future.successful(()))
          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))
          when(mockView.apply(any(), any(), any(), any())(any(), any()))
            .thenReturn(HtmlFormat.raw("<p>Test view</p>"))

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[EmailService].toInstance(mockEmailService),
              bind[SubscriptionService].toInstance(mockSubscriptionService),
              bind[RegistrationConfirmationView].toInstance(mockView)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.RegistrationConfirmationController.onPageLoad().url)
            val result  = route(application, request).value

            status(result) mustEqual OK

            verify(mockEmailService).sendRegistrationConfirmation(
              eqTo(List("orgwithout@test.com")),
              eqTo("XXCAR0012345678"),
              eqTo(None)
            )
          }
        }
      }

      "when user is IndWithoutId" - {
        "must return OK and render the view with no ID number" in {

          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithoutId))
            .withPage(IndividualEmailPage, "indwithout@test.com")

          when(mockEmailService.sendRegistrationConfirmation(any(), any(), any()))
            .thenReturn(Future.successful(()))
          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))
          when(mockView.apply(any(), any(), any(), any())(any(), any()))
            .thenReturn(HtmlFormat.raw("<p>Test view</p>"))

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[EmailService].toInstance(mockEmailService),
              bind[SubscriptionService].toInstance(mockSubscriptionService),
              bind[RegistrationConfirmationView].toInstance(mockView)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.RegistrationConfirmationController.onPageLoad().url)
            val result  = route(application, request).value

            status(result) mustEqual OK

            verify(mockEmailService).sendRegistrationConfirmation(
              eqTo(List("indwithout@test.com")),
              eqTo("XXCAR0012345678"),
              eqTo(None)
            )
          }
        }
      }

      "when user is IndWithUtr (Sole Trader)" - {
        "must return OK and render the view with UTR" in {

          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(IndWithUtr))
            .withPage(IndividualEmailPage, "soletrader@test.com")
            .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("9876543210"))

          when(mockEmailService.sendRegistrationConfirmation(any(), any(), any()))
            .thenReturn(Future.successful(()))
          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))
          when(mockView.apply(any(), any(), any(), any())(any(), any()))
            .thenReturn(HtmlFormat.raw("<p>Test view</p>"))

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[EmailService].toInstance(mockEmailService),
              bind[SubscriptionService].toInstance(mockSubscriptionService),
              bind[RegistrationConfirmationView].toInstance(mockView)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.RegistrationConfirmationController.onPageLoad().url)
            val result  = route(application, request).value

            status(result) mustEqual OK

            verify(mockEmailService).sendRegistrationConfirmation(
              eqTo(List("soletrader@test.com")),
              eqTo("XXCAR0012345678"),
              eqTo(Some("9876543210"))
            )
          }
        }
      }

      "error scenarios" - {

        "must redirect to journey recovery when primary email is missing" in {

          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(OrgWithUtr))
            .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("1234567890"))

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[EmailService].toInstance(mockEmailService),
              bind[SubscriptionService].toInstance(mockSubscriptionService)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.RegistrationConfirmationController.onPageLoad().url)
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "must redirect to journey recovery when email service fails" in {

          val userAnswers = emptyUserAnswers
            .copy(journeyType = Some(OrgWithUtr))
            .withPage(FirstContactEmailPage, "org@test.com")
            .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("1234567890"))

          when(mockEmailService.sendRegistrationConfirmation(any(), any(), any()))
            .thenReturn(Future.failed(new RuntimeException("Email service failed")))

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[EmailService].toInstance(mockEmailService),
              bind[SubscriptionService].toInstance(mockSubscriptionService)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.RegistrationConfirmationController.onPageLoad().url)
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }
    }
  }
}
