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
import config.FrontendAppConfig
import models.JourneyType.*
import models.{Name, RegistrationType, SubscriptionId, UniqueTaxpayerReference, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, reset, verify, when}
import pages.individual.{IndividualEmailPage, WhatIsYourNameIndividualPage}
import pages.individualWithoutId.IndWithoutNinoNamePage
import pages.organisation.*
import pages.{PasscodePage, RegistrationTypePage, SubmissionSucceededPage}
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{ContactEmailInfo, EmailService}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.RegistrationConfirmationView

import scala.concurrent.Future

class RegistrationConfirmationControllerSpec extends SpecBase {

  private val mockEmailService: EmailService   = mock[EmailService]
  private val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  private val subscriptionId: SubscriptionId   = SubscriptionId("XCARF0012345678")
  private val passcode: String                 = "Test"
  private val routingUrl: String               = "http://localhost:17002/manage-your-rcasps"
  private val passcodeRoutingUrl: String       = s"$routingUrl/passcode/$passcode"

  override def beforeEach(): Unit = {
    reset(mockEmailService)
    super.beforeEach()
  }

  def buildApplication(userAnswers: Option[UserAnswers]): Application =
    applicationBuilder(userAnswers = userAnswers)
      .overrides(bind[EmailService].toInstance(mockEmailService), bind[FrontendAppConfig].toInstance(mockAppConfig))
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

    when(mockAppConfig.managementRoutingUrl).thenReturn(routingUrl)
    when(mockAppConfig.feedbackUrl(any())).thenReturn("foo")

    "onPageLoad" - {

      "must return OK and render view when submission already succeeded (OrgWithUtr with LimitedCompany)" in {
        val userAnswers =
          emptyUserAnswers
            .copy(journeyType = Some(OrgWithUtr), subscriptionId = Some(subscriptionId))
            .withPage(FirstContactNamePage, "John Doe")
            .withPage(FirstContactEmailPage, "org@test.com")
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, "Jane Smith")
            .withPage(OrganisationSecondContactEmailPage, "org2@test.com")
            .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("1234567890"))
            .withPage(RegistrationTypePage, RegistrationType.LimitedCompany)
            .withPage(SubmissionSucceededPage, true)
            .copy(isCtAutoMatched = true)

        stubSessionSave()

        when(mockAppConfig.passcodeCheckEnabled).thenReturn(false)
        when(
          mockEmailService.sendEmails(any[List[ContactEmailInfo]], any[Option[String]], any[Boolean])(
            any[HeaderCarrier]
          )
        ).thenReturn(Future.successful(()))

        val application = buildApplication(Some(userAnswers))
        val view        = application.injector.instanceOf[RegistrationConfirmationView]

        running(application) {
          val result = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            subscriptionId = subscriptionId.value,
            emailAddresses = List("org@test.com", "org2@test.com"),
            addProviderUrl = routingUrl
          )(request, messages(application)).toString

          verify(mockEmailService).sendEmails(
            any[List[ContactEmailInfo]],
            eqTo(Some(subscriptionId.value)),
            eqTo(true)
          )(any[HeaderCarrier])
        }
      }

      "must return OK and render view when submission already succeeded (OrgWithUtr with LimitedCompany) and passcode check is enabled" in {
        val userAnswers =
          emptyUserAnswers
            .copy(journeyType = Some(OrgWithUtr), subscriptionId = Some(subscriptionId))
            .withPage(FirstContactNamePage, "John Doe")
            .withPage(FirstContactEmailPage, "org@test.com")
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, "Jane Smith")
            .withPage(OrganisationSecondContactEmailPage, "org2@test.com")
            .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("1234567890"))
            .withPage(RegistrationTypePage, RegistrationType.LimitedCompany)
            .withPage(SubmissionSucceededPage, true)
            .withPage(PasscodePage, passcode)
            .copy(isCtAutoMatched = true)

        stubSessionSave()

        when(mockAppConfig.passcodeCheckEnabled).thenReturn(true)
        when(
          mockEmailService.sendEmails(any[List[ContactEmailInfo]], any[Option[String]], any[Boolean])(
            any[HeaderCarrier]
          )
        ).thenReturn(Future.successful(()))

        val application = buildApplication(Some(userAnswers))
        val view        = application.injector.instanceOf[RegistrationConfirmationView]

        running(application) {
          val result = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            subscriptionId = subscriptionId.value,
            emailAddresses = List("org@test.com", "org2@test.com"),
            addProviderUrl = passcodeRoutingUrl
          )(request, messages(application)).toString

          verify(mockEmailService).sendEmails(
            any[List[ContactEmailInfo]],
            eqTo(Some(subscriptionId.value)),
            eqTo(true)
          )(any[HeaderCarrier])
        }
      }

      "must return OK and render view for organisation with UTR and allowed registration type" in {
        stubSessionSave()

        when(mockAppConfig.passcodeCheckEnabled).thenReturn(false)
        when(
          mockEmailService.sendEmails(any[List[ContactEmailInfo]], any[Option[String]], any[Boolean])(
            any[HeaderCarrier]
          )
        ).thenReturn(Future.successful(()))

        val userAnswers =
          emptyUserAnswers
            .copy(journeyType = Some(OrgWithUtr), subscriptionId = Some(subscriptionId))
            .withPage(FirstContactNamePage, "John Doe")
            .withPage(FirstContactEmailPage, "org@test.com")
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, "Jane Smith")
            .withPage(OrganisationSecondContactEmailPage, "org2@test.com")
            .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("1234567890"))
            .withPage(RegistrationTypePage, RegistrationType.Partnership)
            .copy(isCtAutoMatched = true)

        val application = buildApplication(Some(userAnswers))
        val view        = application.injector.instanceOf[RegistrationConfirmationView]

        running(application) {
          val result = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            subscriptionId = subscriptionId.value,
            emailAddresses = List("org@test.com", "org2@test.com"),
            addProviderUrl = routingUrl
          )(request, messages(application)).toString

          verify(mockEmailService).sendEmails(
            any[List[ContactEmailInfo]],
            eqTo(Some(subscriptionId.value)),
            eqTo(false)
          )(any[HeaderCarrier])
        }
      }

      "must return OK and render view for organisation with UTR and allowed registration type when passcode check is enabled" in {
        stubSessionSave()

        when(mockAppConfig.passcodeCheckEnabled).thenReturn(true)
        when(
          mockEmailService.sendEmails(any[List[ContactEmailInfo]], any[Option[String]], any[Boolean])(
            any[HeaderCarrier]
          )
        ).thenReturn(Future.successful(()))

        val userAnswers =
          emptyUserAnswers
            .copy(journeyType = Some(OrgWithUtr), subscriptionId = Some(subscriptionId))
            .withPage(FirstContactNamePage, "John Doe")
            .withPage(FirstContactEmailPage, "org@test.com")
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, "Jane Smith")
            .withPage(OrganisationSecondContactEmailPage, "org2@test.com")
            .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("1234567890"))
            .withPage(RegistrationTypePage, RegistrationType.Partnership)
            .withPage(PasscodePage, passcode)
            .copy(isCtAutoMatched = true)

        val application = buildApplication(Some(userAnswers))
        val view        = application.injector.instanceOf[RegistrationConfirmationView]

        running(application) {
          val result = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            subscriptionId = subscriptionId.value,
            emailAddresses = List("org@test.com", "org2@test.com"),
            addProviderUrl = passcodeRoutingUrl
          )(request, messages(application)).toString

          verify(mockEmailService).sendEmails(
            any[List[ContactEmailInfo]],
            eqTo(Some(subscriptionId.value)),
            eqTo(false)
          )(any[HeaderCarrier])
        }
      }

      "must return OK and render view for organisation with SoleTrader (no CARF reference)" in {
        stubSessionSave()

        when(mockAppConfig.passcodeCheckEnabled).thenReturn(false)
        when(
          mockEmailService.sendEmails(any[List[ContactEmailInfo]], any[Option[String]], any[Boolean])(
            any[HeaderCarrier]
          )
        ).thenReturn(Future.successful(()))

        val userAnswers =
          emptyUserAnswers
            .copy(journeyType = Some(IndWithUtr), subscriptionId = Some(subscriptionId))
            .withPage(WhatIsYourNamePage, Name("John", "Doe"))
            .withPage(IndividualEmailPage, "timmy@example.com")
            .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("1234567890"))
            .withPage(RegistrationTypePage, RegistrationType.SoleTrader)
            .copy(isCtAutoMatched = false)

        val application = buildApplication(Some(userAnswers))
        val view        = application.injector.instanceOf[RegistrationConfirmationView]

        running(application) {
          val result = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            subscriptionId = subscriptionId.value,
            emailAddresses = List("timmy@example.com"),
            addProviderUrl = routingUrl
          )(request, messages(application)).toString

          verify(mockEmailService).sendEmails(
            any[List[ContactEmailInfo]],
            eqTo(None),
            eqTo(false)
          )(any[HeaderCarrier])
        }
      }

      "must return OK and render view for organisation with SoleTrader (no CARF reference) when passcode check is enabled" in {
        stubSessionSave()

        when(mockAppConfig.passcodeCheckEnabled).thenReturn(true)
        when(
          mockEmailService.sendEmails(any[List[ContactEmailInfo]], any[Option[String]], any[Boolean])(
            any[HeaderCarrier]
          )
        ).thenReturn(Future.successful(()))

        val userAnswers =
          emptyUserAnswers
            .copy(journeyType = Some(IndWithUtr), subscriptionId = Some(subscriptionId))
            .withPage(WhatIsYourNamePage, Name("John", "Doe"))
            .withPage(IndividualEmailPage, "timmy@example.com")
            .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("1234567890"))
            .withPage(RegistrationTypePage, RegistrationType.SoleTrader)
            .withPage(PasscodePage, passcode)
            .copy(isCtAutoMatched = false)

        val application = buildApplication(Some(userAnswers))
        val view        = application.injector.instanceOf[RegistrationConfirmationView]

        running(application) {
          val result = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            subscriptionId = subscriptionId.value,
            emailAddresses = List("timmy@example.com"),
            addProviderUrl = passcodeRoutingUrl
          )(request, messages(application)).toString

          verify(mockEmailService).sendEmails(
            any[List[ContactEmailInfo]],
            eqTo(None),
            eqTo(false)
          )(any[HeaderCarrier])
        }
      }

      "must return OK and render view for organisation without ID" in {
        stubSessionSave()

        when(mockAppConfig.passcodeCheckEnabled).thenReturn(false)
        when(
          mockEmailService.sendEmails(any[List[ContactEmailInfo]], any[Option[String]], any[Boolean])(
            any[HeaderCarrier]
          )
        ).thenReturn(Future.successful(()))

        val userAnswers =
          emptyUserAnswers
            .copy(journeyType = Some(OrgWithoutId), subscriptionId = Some(subscriptionId))
            .withPage(FirstContactNamePage, "John Doe")
            .withPage(FirstContactEmailPage, "orgwithout@test.com")
            .withPage(OrganisationHaveSecondContactPage, false)
            .copy(isCtAutoMatched = false)

        val application = buildApplication(Some(userAnswers))
        val view        = application.injector.instanceOf[RegistrationConfirmationView]

        running(application) {
          val result = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            subscriptionId = subscriptionId.value,
            emailAddresses = List("orgwithout@test.com"),
            addProviderUrl = routingUrl
          )(request, messages(application)).toString

          verify(mockEmailService).sendEmails(
            any[List[ContactEmailInfo]],
            eqTo(Some(subscriptionId.value)),
            eqTo(false)
          )(any[HeaderCarrier])
        }
      }

      "must return OK and render view for organisation without ID when passcode check is enabled" in {
        stubSessionSave()

        when(mockAppConfig.passcodeCheckEnabled).thenReturn(true)
        when(
          mockEmailService.sendEmails(any[List[ContactEmailInfo]], any[Option[String]], any[Boolean])(
            any[HeaderCarrier]
          )
        ).thenReturn(Future.successful(()))

        val userAnswers =
          emptyUserAnswers
            .copy(journeyType = Some(OrgWithoutId), subscriptionId = Some(subscriptionId))
            .withPage(FirstContactNamePage, "John Doe")
            .withPage(FirstContactEmailPage, "orgwithout@test.com")
            .withPage(OrganisationHaveSecondContactPage, false)
            .withPage(PasscodePage, passcode)
            .copy(isCtAutoMatched = false)

        val application = buildApplication(Some(userAnswers))
        val view        = application.injector.instanceOf[RegistrationConfirmationView]

        running(application) {
          val result = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            subscriptionId = subscriptionId.value,
            emailAddresses = List("orgwithout@test.com"),
            addProviderUrl = passcodeRoutingUrl
          )(request, messages(application)).toString

          verify(mockEmailService).sendEmails(
            any[List[ContactEmailInfo]],
            eqTo(Some(subscriptionId.value)),
            eqTo(false)
          )(any[HeaderCarrier])
        }
      }

      "must return OK and render view for IndWithNino (no CARF reference)" in {
        reset(mockEmailService, mockSessionRepository)
        stubSessionSave()

        when(mockEmailService.sendEmails(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(()))

        val userAnswers =
          emptyUserAnswers
            .copy(journeyType = Some(IndWithNino), subscriptionId = Some(subscriptionId))
            .withPage(WhatIsYourNameIndividualPage, Name("John", "Smith"))
            .withPage(IndividualEmailPage, "individual@test.com")

        val application = buildApplication(Some(userAnswers))

        running(application) {
          val result = route(application, request).value

          status(result) mustEqual OK

          verify(mockEmailService).sendEmails(
            any[List[ContactEmailInfo]],
            eqTo(None),
            eqTo(false)
          )(any[HeaderCarrier])
        }
      }

      "must return OK and render view for IndWithUtr (no CARF reference)" in {
        reset(mockEmailService, mockSessionRepository)
        stubSessionSave()

        when(mockAppConfig.passcodeCheckEnabled).thenReturn(false)
        when(mockEmailService.sendEmails(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(()))

        val userAnswers =
          emptyUserAnswers
            .copy(journeyType = Some(IndWithUtr), subscriptionId = Some(subscriptionId))
            .withPage(WhatIsYourNamePage, Name("John", "Smith"))
            .withPage(IndividualEmailPage, "soletrader@test.com")

        val application = buildApplication(Some(userAnswers))

        running(application) {
          val result = route(application, request).value

          status(result) mustEqual OK

          verify(mockEmailService).sendEmails(
            any[List[ContactEmailInfo]],
            eqTo(None),
            eqTo(false)
          )(any[HeaderCarrier])
        }
      }

      "must return OK and render view for individual without ID journeys (no CARF reference)" in {
        reset(mockEmailService, mockSessionRepository)

        stubSessionSave()

        when(mockAppConfig.passcodeCheckEnabled).thenReturn(false)
        when(
          mockEmailService.sendEmails(any[List[ContactEmailInfo]], any[Option[String]], any[Boolean])(
            any[HeaderCarrier]
          )
        ).thenReturn(Future.successful(()))

        val userAnswers =
          emptyUserAnswers
            .copy(journeyType = Some(IndWithoutId), subscriptionId = Some(subscriptionId))
            .withPage(IndWithoutNinoNamePage, Name("John", "Smith"))
            .withPage(IndividualEmailPage, "indwithout@test.com")

        val application = buildApplication(Some(userAnswers))
        val view        = application.injector.instanceOf[RegistrationConfirmationView]

        running(application) {
          val result = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            subscriptionId = subscriptionId.value,
            emailAddresses = List("indwithout@test.com"),
            addProviderUrl = routingUrl
          )(request, messages(application)).toString

          verify(mockEmailService).sendEmails(
            any[List[ContactEmailInfo]],
            eqTo(None),
            eqTo(false)
          )(any[HeaderCarrier])
        }
      }

      "must return OK and render view for individual without ID journeys (no CARF reference) when passcode check is enabled" in {
        reset(mockEmailService, mockSessionRepository)

        stubSessionSave()

        when(mockAppConfig.passcodeCheckEnabled).thenReturn(true)
        when(
          mockEmailService.sendEmails(any[List[ContactEmailInfo]], any[Option[String]], any[Boolean])(
            any[HeaderCarrier]
          )
        ).thenReturn(Future.successful(()))

        val userAnswers =
          emptyUserAnswers
            .copy(journeyType = Some(IndWithoutId), subscriptionId = Some(subscriptionId))
            .withPage(IndWithoutNinoNamePage, Name("John", "Smith"))
            .withPage(IndividualEmailPage, "indwithout@test.com")
            .withPage(PasscodePage, passcode)

        val application = buildApplication(Some(userAnswers))
        val view        = application.injector.instanceOf[RegistrationConfirmationView]

        running(application) {
          val result = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            subscriptionId = subscriptionId.value,
            emailAddresses = List("indwithout@test.com"),
            addProviderUrl = passcodeRoutingUrl
          )(request, messages(application)).toString

          verify(mockEmailService).sendEmails(
            any[List[ContactEmailInfo]],
            eqTo(None),
            eqTo(false)
          )(any[HeaderCarrier])
        }
      }

      "must redirect when organisation email missing" in {
        Seq(OrgWithUtr, OrgWithoutId).foreach { journey =>
          val userAnswers =
            emptyUserAnswers
              .copy(journeyType = Some(journey), subscriptionId = Some(subscriptionId))
              .withPage(FirstContactNamePage, "John Doe")

          val application = buildApplication(Some(userAnswers))

          running(application) {
            redirectAssertion(route(application, request).value)

            verify(mockEmailService, never()).sendEmails(
              any[List[ContactEmailInfo]],
              any[Option[String]],
              any[Boolean]
            )(any[HeaderCarrier])
          }
        }
      }

      "must redirect when individual email missing" in {
        Seq(IndWithNino, IndWithUtr, IndWithoutId).foreach { journey =>
          val base =
            emptyUserAnswers
              .copy(journeyType = Some(journey), subscriptionId = Some(subscriptionId))

          val userAnswers = journey match {
            case IndWithoutId =>
              base.withPage(IndWithoutNinoNamePage, Name("John", "Smith"))
            case _            =>
              base.withPage(WhatIsYourNameIndividualPage, Name("John", "Smith"))
          }

          val application = buildApplication(Some(userAnswers))

          running(application) {
            redirectAssertion(route(application, request).value)

            verify(mockEmailService, never()).sendEmails(
              any[List[ContactEmailInfo]],
              any[Option[String]],
              any[Boolean]
            )(any[HeaderCarrier])
          }
        }
      }

      "must redirect when journeyType is missing" in {
        val userAnswers = emptyUserAnswers.copy(subscriptionId = Some(subscriptionId))
        val application = buildApplication(Some(userAnswers))

        running(application) {
          redirectAssertion(route(application, request).value)

          verify(mockEmailService, never()).sendEmails(
            any[List[ContactEmailInfo]],
            any[Option[String]],
            any[Boolean]
          )(any[HeaderCarrier])
        }
      }

      "must redirect when subscriptionId is missing" in {
        val userAnswers = emptyUserAnswers.copy(journeyType = Some(OrgWithUtr))

        val application = buildApplication(Some(userAnswers))

        running(application) {
          redirectAssertion(route(application, request).value)

          verify(mockEmailService, never()).sendEmails(
            any[List[ContactEmailInfo]],
            any[Option[String]],
            any[Boolean]
          )(any[HeaderCarrier])
        }
      }

      "must still return OK even if email sending fails" in {

        val userAnswers =
          emptyUserAnswers
            .copy(journeyType = Some(OrgWithUtr), subscriptionId = Some(subscriptionId))
            .withPage(FirstContactNamePage, "John Doe")
            .withPage(FirstContactEmailPage, "org@test.com")

        stubSessionSave()

        when(
          mockEmailService.sendEmails(
            any[List[ContactEmailInfo]],
            any[Option[String]],
            any[Boolean]
          )(any[HeaderCarrier])
        ).thenReturn(Future.successful(()))

        val application = buildApplication(Some(userAnswers))

        running(application) {
          val result = route(application, request).value

          status(result) mustEqual OK
          verify(mockEmailService).sendEmails(
            any[List[ContactEmailInfo]],
            eqTo(Some(subscriptionId.value)),
            eqTo(false)
          )(any[HeaderCarrier])
        }
      }
    }
  }
}
