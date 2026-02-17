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
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.inject.bind
import pages.individual.{IndividualEmailPage, NiNumberPage}
import pages.organisation.{FirstContactEmailPage, OrganisationHaveSecondContactPage, OrganisationSecondContactEmailPage, UniqueTaxpayerReferenceInUserAnswers}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.EmailService
import views.html.RegistrationConfirmationView

import scala.concurrent.Future

class RegistrationConfirmationControllerSpec extends SpecBase with MockitoSugar {

  private val mockEmailService      = mock[EmailService]
  private val mockSessionRepository = mock[SessionRepository]

  def buildApplication(userAnswers: Option[UserAnswers]) =
    applicationBuilder(userAnswers = userAnswers)
      .overrides(
        bind[EmailService].toInstance(mockEmailService),
        bind[SessionRepository].toInstance(mockSessionRepository)
      )
      .build()

  private val subscriptionId = "XXCAR0012345678"

  "RegistrationConfirmationController" - {

    "onPageLoad" - {

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

        val application = buildApplication(Some(userAnswers))
        val view        = application.injector.instanceOf[RegistrationConfirmationView]

        running(application) {
          val request = FakeRequest(GET, routes.RegistrationConfirmationController.onPageLoad().url)
          val result  = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            subscriptionId = subscriptionId,
            emailAddresses = List("org@test.com", "org2@test.com"),
            addProviderUrl = controllers.routes.PlaceholderController
              .onPageLoad("redirect to /report-for-registered-business (ct automatch) (CARF-368)")
              .url
          )(request, messages(application)).toString
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

        val application = buildApplication(Some(userAnswers))
        val view        = application.injector.instanceOf[RegistrationConfirmationView]

        running(application) {
          val request = FakeRequest(GET, routes.RegistrationConfirmationController.onPageLoad().url)
          val result  = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            subscriptionId = subscriptionId,
            emailAddresses = List("orgwithout@test.com"),
            addProviderUrl = controllers.routes.PlaceholderController
              .onPageLoad("redirect to /organisation-or-individual (non-automatch) (CARF-368)")
              .url
          )(request, messages(application)).toString
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

        val application = buildApplication(Some(userAnswers))
        val view        = application.injector.instanceOf[RegistrationConfirmationView]

        running(application) {
          val request = FakeRequest(GET, routes.RegistrationConfirmationController.onPageLoad().url)
          val result  = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            subscriptionId = subscriptionId,
            emailAddresses = List("individual@test.com"),
            addProviderUrl = controllers.routes.PlaceholderController
              .onPageLoad("redirect to /organisation-or-individual (individual) (CARF-368)")
              .url
          )(request, messages(application)).toString
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

        val application = buildApplication(Some(userAnswers))
        val view        = application.injector.instanceOf[RegistrationConfirmationView]

        running(application) {
          val request = FakeRequest(GET, routes.RegistrationConfirmationController.onPageLoad().url)
          val result  = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            subscriptionId = subscriptionId,
            emailAddresses = List("indwithout@test.com"),
            addProviderUrl = controllers.routes.PlaceholderController
              .onPageLoad("redirect to /organisation-or-individual (individual) (CARF-368)")
              .url
          )(request, messages(application)).toString
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

        val application = buildApplication(Some(userAnswers))
        val view        = application.injector.instanceOf[RegistrationConfirmationView]

        running(application) {
          val request = FakeRequest(GET, routes.RegistrationConfirmationController.onPageLoad().url)
          val result  = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            subscriptionId = subscriptionId,
            emailAddresses = List("soletrader@test.com"),
            addProviderUrl = controllers.routes.PlaceholderController
              .onPageLoad("redirect to /organisation-or-individual (individual) (CARF-368)")
              .url
          )(request, messages(application)).toString
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
