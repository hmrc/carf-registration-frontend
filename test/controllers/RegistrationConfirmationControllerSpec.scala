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
import pages.organisation.{FirstContactEmailPage, OrganisationHaveSecondContactPage, OrganisationSecondContactEmailPage, UniqueTaxpayerReferenceInUserAnswers}
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

      def buildApplication(userAnswers: Option[UserAnswers]) =
        applicationBuilder(userAnswers = userAnswers)
          .overrides(
            bind[EmailService].toInstance(mockEmailService),
            bind[SubscriptionService].toInstance(mockSubscriptionService),
            bind[RegistrationConfirmationView].toInstance(mockView)
          )
          .build()

      val subscriptionId = "XXCAR0012345678"

      def mockViewApply(): Unit =
        when(mockView.apply(any[String], any[List[String]], any[String])(any(), any()))
          .thenReturn(HtmlFormat.raw("<p>Test view</p>"))

      "must return OK and render view for OrgWithUtr" in {

        val userAnswers = emptyUserAnswers
          .copy(journeyType = Some(OrgWithUtr))
          .withPage(FirstContactEmailPage, "org@test.com")
          .withPage(OrganisationHaveSecondContactPage, true)
          .withPage(OrganisationSecondContactEmailPage, "org2@test.com")
          .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("1234567890"))
          .copy(isCtAutoMatched = true)

        when(mockEmailService.sendRegistrationConfirmation(any[List[String]], any[String], any[Option[String]]))
          .thenReturn(Future.successful(()))
        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))
        mockViewApply()

        val application = buildApplication(Some(userAnswers))

        running(application) {
          val request = FakeRequest(GET, routes.RegistrationConfirmationController.onPageLoad().url)
          val result  = route(application, request).value

          status(result) mustEqual OK

          val expectedEmails = List("org@test.com", "org2@test.com")

          verify(mockEmailService).sendRegistrationConfirmation(
            eqTo(expectedEmails),
            eqTo(subscriptionId),
            eqTo(Some("1234567890"))
          )

          val expectedUrl = controllers.routes.PlaceholderController
            .onPageLoad("redirect to /report-for-registered-business (ct automatch) (CARF-368)")
            .url

          verify(mockView).apply(
            eqTo(subscriptionId),
            eqTo(expectedEmails),
            eqTo(expectedUrl)
          )(any(), any())

          verify(mockSessionRepository).set(any())
        }
      }

      "must return OK and render view for OrgWithoutId (non-automatch)" in {

        val userAnswers = emptyUserAnswers
          .copy(journeyType = Some(OrgWithoutId))
          .withPage(FirstContactEmailPage, "orgwithout@test.com")
          .withPage(OrganisationHaveSecondContactPage, false)
          .copy(isCtAutoMatched = false)

        when(mockEmailService.sendRegistrationConfirmation(any[List[String]], any[String], any[Option[String]]))
          .thenReturn(Future.successful(()))
        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))
        mockViewApply()

        val application = buildApplication(Some(userAnswers))

        running(application) {
          val request = FakeRequest(GET, routes.RegistrationConfirmationController.onPageLoad().url)
          val result  = route(application, request).value

          status(result) mustEqual OK

          val expectedEmails = List("orgwithout@test.com")

          verify(mockEmailService).sendRegistrationConfirmation(
            eqTo(expectedEmails),
            eqTo(subscriptionId),
            eqTo(None)
          )

          val expectedUrl = controllers.routes.PlaceholderController
            .onPageLoad("redirect to /organisation-or-individual (non-automatch) (CARF-368)")
            .url

          verify(mockView).apply(
            eqTo(subscriptionId),
            eqTo(expectedEmails),
            eqTo(expectedUrl)
          )(any(), any())
        }
      }

      "must return OK and render view for IndWithNino" in {

        val userAnswers = emptyUserAnswers
          .copy(journeyType = Some(IndWithNino))
          .withPage(IndividualEmailPage, "individual@test.com")
          .withPage(NiNumberPage, "AB123456C")

        when(mockEmailService.sendRegistrationConfirmation(any[List[String]], any[String], any[Option[String]]))
          .thenReturn(Future.successful(()))
        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))
        mockViewApply()

        val application = buildApplication(Some(userAnswers))

        running(application) {
          val request = FakeRequest(GET, routes.RegistrationConfirmationController.onPageLoad().url)
          val result  = route(application, request).value

          status(result) mustEqual OK

          val expectedEmails = List("individual@test.com")

          verify(mockEmailService).sendRegistrationConfirmation(
            eqTo(expectedEmails),
            eqTo(subscriptionId),
            eqTo(Some("AB123456C"))
          )

          val expectedUrl = controllers.routes.PlaceholderController
            .url

          verify(mockView).apply(
            eqTo(subscriptionId),
            eqTo(expectedEmails),
            eqTo(expectedUrl)
          )(any(), any())
        }
      }

      "must return OK and render view for IndWithoutId" in {

        val userAnswers = emptyUserAnswers
          .copy(journeyType = Some(IndWithoutId))
          .withPage(IndividualEmailPage, "indwithout@test.com")

        when(mockEmailService.sendRegistrationConfirmation(any[List[String]], any[String], any[Option[String]]))
          .thenReturn(Future.successful(()))
        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))
        mockViewApply()

        val application = buildApplication(Some(userAnswers))

        running(application) {
          val request = FakeRequest(GET, routes.RegistrationConfirmationController.onPageLoad().url)
          val result  = route(application, request).value

          status(result) mustEqual OK

          val expectedEmails = List("indwithout@test.com")

          verify(mockEmailService).sendRegistrationConfirmation(
            eqTo(expectedEmails),
            eqTo(subscriptionId),
            eqTo(None)
          )

          val expectedUrl = controllers.routes.PlaceholderController
            .onPageLoad("redirect to /organisation-or-individual (individual) (CARF-368)")
            .url

          verify(mockView).apply(
            eqTo(subscriptionId),
            eqTo(expectedEmails),
            eqTo(expectedUrl)
          )(any(), any())
        }
      }

      "must return OK and render view for IndWithUtr" in {

        val userAnswers = emptyUserAnswers
          .copy(journeyType = Some(IndWithUtr))
          .withPage(IndividualEmailPage, "soletrader@test.com")
          .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("9876543210"))

        when(mockEmailService.sendRegistrationConfirmation(any[List[String]], any[String], any[Option[String]]))
          .thenReturn(Future.successful(()))
        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))
        mockViewApply()

        val application = buildApplication(Some(userAnswers))

        running(application) {
          val request = FakeRequest(GET, routes.RegistrationConfirmationController.onPageLoad().url)
          val result  = route(application, request).value

          status(result) mustEqual OK

          val expectedEmails = List("soletrader@test.com")

          verify(mockEmailService).sendRegistrationConfirmation(
            eqTo(expectedEmails),
            eqTo(subscriptionId),
            eqTo(Some("9876543210"))
          )

          val expectedUrl = controllers.routes.PlaceholderController
            .onPageLoad("redirect to /organisation-or-individual (individual) (CARF-368)")
            .url

          verify(mockView).apply(
            eqTo(subscriptionId),
            eqTo(expectedEmails),
            eqTo(expectedUrl)
          )(any(), any())
        }
      }

      "must redirect to journey recovery when primary email is missing" in {

        val userAnswers = emptyUserAnswers
          .copy(journeyType = Some(OrgWithUtr))
          .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("1234567890"))

        val application = buildApplication(Some(userAnswers))

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

        when(mockEmailService.sendRegistrationConfirmation(any[List[String]], any[String], any[Option[String]]))
          .thenReturn(Future.failed(new RuntimeException("Email service failed")))

        val application = buildApplication(Some(userAnswers))

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
