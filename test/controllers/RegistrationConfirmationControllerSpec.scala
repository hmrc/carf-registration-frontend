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
import models.JourneyType.*
import models.{Name, SubscriptionId, UniqueTaxpayerReference, UserAnswers}
import org.mockito.Mockito.{atLeastOnce, never, reset, times, verify, when}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.scalatestplus.mockito.MockitoSugar
import pages.SubmissionSucceededPage
import pages.individual.{IndividualEmailPage, NiNumberPage, WhatIsYourNameIndividualPage}
import pages.individualWithoutId.IndWithoutNinoNamePage
import pages.organisation.*
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{ContactEmailInfo, EmailService}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.RegistrationConfirmationView

import scala.concurrent.Future

class RegistrationConfirmationControllerSpec extends SpecBase with MockitoSugar {

  private val mockEmailService = mock[EmailService]
  private val subscriptionId   = SubscriptionId("XCARF0012345678")

  override def beforeEach(): Unit = {
    reset(mockEmailService)
    super.beforeEach()
  }

  def buildApplication(userAnswers: Option[UserAnswers]) =
    applicationBuilder(userAnswers = userAnswers)
      .overrides(bind[EmailService].toInstance(mockEmailService))
      .build()

  private def request = FakeRequest(routes.RegistrationConfirmationController.onPageLoad())

  private def stubSessionSave(): Unit =
    when(mockSessionRepository.set(any[UserAnswers]))
      .thenReturn(Future.successful(true))

  private def redirectAssertion(result: Future[play.api.mvc.Result]) = {
    status(result)                 mustEqual SEE_OTHER
    redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
  }

  "RegistrationConfirmationController" - {

    "onPageLoad" - {

      "must return OK and render view when submission already succeeded (OrgWithUtr)" in {

        val userAnswers =
          emptyUserAnswers
            .copy(journeyType = Some(OrgWithUtr))
            .copy(subscriptionId = Some(subscriptionId))
            .set(FirstContactNamePage, "John Doe")
            .success
            .value
            .set(FirstContactEmailPage, "org@test.com")
            .success
            .value
            .set(OrganisationHaveSecondContactPage, true)
            .success
            .value
            .set(OrganisationSecondContactNamePage, "Jane Smith")
            .success
            .value
            .set(OrganisationSecondContactEmailPage, "org2@test.com")
            .success
            .value
            .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("1234567890"))
            .success
            .value
            .set(SubmissionSucceededPage, true)
            .success
            .value
            .copy(isCtAutoMatched = true)

        val application = buildApplication(Some(userAnswers))
        val view        = application.injector.instanceOf[RegistrationConfirmationView]

        running(application) {
          val result = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            subscriptionId = subscriptionId.value,
            emailAddresses = List("org@test.com", "org2@test.com"),
            addProviderUrl = controllers.routes.PlaceholderController
              .onPageLoad("redirect to /report-for-registered-business (ct automatch) (CARF-368)")
              .url
          )(request, messages(application)).toString

          verify(mockEmailService, never()).sendRegistrationConfirmation(
            any[List[ContactEmailInfo]],
            any[String]
          )(any[HeaderCarrier])
        }
      }

      "must return OK and render view for organisation with UTR" in {

        stubSessionSave()

        when(
          mockEmailService.sendRegistrationConfirmation(
            any[List[ContactEmailInfo]],
            any[String]
          )(any[HeaderCarrier])
        ).thenReturn(Future.successful(()))

        val userAnswers =
          emptyUserAnswers
            .copy(journeyType = Some(OrgWithUtr))
            .copy(subscriptionId = Some(subscriptionId))
            .set(FirstContactNamePage, "John Doe")
            .success
            .value
            .set(FirstContactEmailPage, "org@test.com")
            .success
            .value
            .set(OrganisationHaveSecondContactPage, true)
            .success
            .value
            .set(OrganisationSecondContactNamePage, "Jane Smith")
            .success
            .value
            .set(OrganisationSecondContactEmailPage, "org2@test.com")
            .success
            .value
            .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("1234567890"))
            .success
            .value
            .copy(isCtAutoMatched = true)

        val application = buildApplication(Some(userAnswers))
        val view        = application.injector.instanceOf[RegistrationConfirmationView]

        running(application) {
          val result = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            subscriptionId = subscriptionId.value,
            emailAddresses = List("org@test.com", "org2@test.com"),
            addProviderUrl = controllers.routes.PlaceholderController
              .onPageLoad("redirect to /report-for-registered-business (ct automatch) (CARF-368)")
              .url
          )(request, messages(application)).toString

          status(result) mustEqual OK

          verify(mockEmailService, atLeastOnce()).sendRegistrationConfirmation(
            any[List[ContactEmailInfo]],
            any[String]
          )(any[HeaderCarrier])
        }
      }

      "must return OK and render view for organisation without ID" in {

        stubSessionSave()

        when(
          mockEmailService.sendRegistrationConfirmation(
            any[List[ContactEmailInfo]],
            any[String]
          )(any[HeaderCarrier])
        ).thenReturn(Future.successful(()))

        val userAnswers =
          emptyUserAnswers
            .copy(journeyType = Some(OrgWithoutId))
            .copy(subscriptionId = Some(subscriptionId))
            .set(FirstContactNamePage, "John Doe")
            .success
            .value
            .set(FirstContactEmailPage, "orgwithout@test.com")
            .success
            .value
            .set(OrganisationHaveSecondContactPage, false)
            .success
            .value
            .copy(isCtAutoMatched = false)

        val application = buildApplication(Some(userAnswers))
        val view        = application.injector.instanceOf[RegistrationConfirmationView]

        running(application) {
          val result = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            subscriptionId = subscriptionId.value,
            emailAddresses = List("orgwithout@test.com"),
            addProviderUrl = controllers.routes.PlaceholderController
              .onPageLoad("redirect to /organisation-or-individual (non-automatch) (CARF-368)")
              .url
          )(request, messages(application)).toString

          verify(mockEmailService, atLeastOnce()).sendRegistrationConfirmation(
            any[List[ContactEmailInfo]],
            any[String]
          )(any[HeaderCarrier])
        }
      }

      "must return OK and render view for individual journeys with ID (NINO/UTR)" in {

        Seq(
          (IndWithNino, "individual@test.com"),
          (IndWithUtr, "soletrader@test.com")
        ).foreach { case (journey, email) =>
          reset(mockEmailService, mockSessionRepository)
          stubSessionSave()

          when(
            mockEmailService.sendRegistrationConfirmation(
              any[List[ContactEmailInfo]],
              any[String]
            )(any[HeaderCarrier])
          ).thenReturn(Future.successful(()))

          val base =
            emptyUserAnswers
              .copy(journeyType = Some(journey))
              .copy(subscriptionId = Some(subscriptionId))
              .set(WhatIsYourNameIndividualPage, Name("John", "Smith"))
              .success
              .value
              .set(IndividualEmailPage, email)
              .success
              .value

          val userAnswers = journey match {
            case IndWithNino =>
              base.set(NiNumberPage, "AB123456C").success.value
            case IndWithUtr  =>
              base.set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("9876543210")).success.value
          }

          val application = buildApplication(Some(userAnswers))

          val view = application.injector.instanceOf[RegistrationConfirmationView]

          running(application) {
            val result = route(application, request).value

            status(result)          mustEqual OK
            contentAsString(result) mustEqual view(
              subscriptionId = subscriptionId.value,
              emailAddresses = List(email),
              addProviderUrl = controllers.routes.PlaceholderController
                .onPageLoad("redirect to /organisation-or-individual (individual) (CARF-368)")
                .url
            )(request, messages(application)).toString

            verify(mockEmailService, atLeastOnce()).sendRegistrationConfirmation(
              any[List[ContactEmailInfo]],
              any[String]
            )(any[HeaderCarrier])
          }
        }
      }

      "must return OK and render view for individual without ID journeys" in {

        reset(mockEmailService, mockSessionRepository)
        stubSessionSave()

        when(
          mockEmailService.sendRegistrationConfirmation(
            any[List[ContactEmailInfo]],
            any[String]
          )(any[HeaderCarrier])
        ).thenReturn(Future.successful(()))

        val userAnswers =
          emptyUserAnswers
            .copy(journeyType = Some(IndWithoutId))
            .copy(subscriptionId = Some(subscriptionId))
            .set(IndWithoutNinoNamePage, Name("John", "Smith"))
            .success
            .value
            .set(IndividualEmailPage, "indwithout@test.com")
            .success
            .value

        val application = buildApplication(Some(userAnswers))
        val view        = application.injector.instanceOf[RegistrationConfirmationView]

        running(application) {
          val result = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            subscriptionId = subscriptionId.value,
            emailAddresses = List("indwithout@test.com"),
            addProviderUrl = controllers.routes.PlaceholderController
              .onPageLoad("redirect to /organisation-or-individual (individual) (CARF-368)")
              .url
          )(request, messages(application)).toString

          verify(mockEmailService, atLeastOnce()).sendRegistrationConfirmation(
            any[List[ContactEmailInfo]],
            any[String]
          )(any[HeaderCarrier])
        }
      }

      "must redirect when organisation email missing" in {

        Seq(OrgWithUtr, OrgWithoutId).foreach { journey =>

          val userAnswers =
            emptyUserAnswers
              .copy(journeyType = Some(journey))
              .copy(subscriptionId = Some(subscriptionId))
              .set(FirstContactNamePage, "John Doe")
              .success
              .value

          val application = buildApplication(Some(userAnswers))

          running(application) {
            redirectAssertion(route(application, request).value)

            verify(mockEmailService, never()).sendRegistrationConfirmation(
              any[List[ContactEmailInfo]],
              any[String]
            )(any[HeaderCarrier])
          }
        }
      }

      "must redirect when individual email missing" in {

        Seq(IndWithNino, IndWithUtr, IndWithoutId).foreach { journey =>

          val base =
            emptyUserAnswers
              .copy(journeyType = Some(journey))
              .copy(subscriptionId = Some(subscriptionId))

          val userAnswers = journey match {
            case IndWithoutId =>
              base
                .set(IndWithoutNinoNamePage, Name("John", "Smith"))
                .success
                .value

            case _ =>
              base
                .set(WhatIsYourNameIndividualPage, Name("John", "Smith"))
                .success
                .value
          }

          val application = buildApplication(Some(userAnswers))

          running(application) {
            redirectAssertion(route(application, request).value)

            verify(mockEmailService, never()).sendRegistrationConfirmation(
              any[List[ContactEmailInfo]],
              any[String]
            )(any[HeaderCarrier])
          }
        }
      }

      "must redirect when journeyType is missing" in {
        val userAnswers = emptyUserAnswers.copy(subscriptionId = Some(subscriptionId))

        val application = buildApplication(Some(userAnswers))

        running(application) {
          redirectAssertion(route(application, request).value)

          verify(mockEmailService, never()).sendRegistrationConfirmation(
            any[List[ContactEmailInfo]],
            any[String]
          )(any[HeaderCarrier])
        }
      }

      "must redirect when subscriptionId is missing" in {
        val userAnswers = emptyUserAnswers.copy(journeyType = Some(OrgWithUtr))

        val application = buildApplication(Some(userAnswers))

        running(application) {
          redirectAssertion(route(application, request).value)

          verify(mockEmailService, never()).sendRegistrationConfirmation(
            any[List[ContactEmailInfo]],
            any[String]
          )(any[HeaderCarrier])
        }
      }

      "must redirect when email service fails" in {

        val userAnswers =
          emptyUserAnswers
            .copy(journeyType = Some(OrgWithUtr))
            .copy(subscriptionId = Some(subscriptionId))
            .set(FirstContactNamePage, "John Doe")
            .success
            .value
            .set(FirstContactEmailPage, "org@test.com")
            .success
            .value
            .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("1234567890"))
            .success
            .value

        when(
          mockEmailService.sendRegistrationConfirmation(
            any[List[ContactEmailInfo]],
            any[String]
          )(any[HeaderCarrier])
        ).thenReturn(Future.failed(new RuntimeException("Email service failed")))

        val application = buildApplication(Some(userAnswers))

        running(application) {
          redirectAssertion(route(application, request).value)

          verify(mockEmailService, atLeastOnce()).sendRegistrationConfirmation(
            any[List[ContactEmailInfo]],
            any[String]
          )(any[HeaderCarrier])
        }
      }
    }
  }
}
