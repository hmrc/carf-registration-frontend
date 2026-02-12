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
import models.UserAnswers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.*
import pages.individual.{IndividualEmailPage, NiNumberPage}
import pages.organisation.{FirstContactEmailPage, OrganisationSecondContactEmailPage, UniqueTaxpayerReferenceInUserAnswers}
import play.api.test.Helpers.*
import play.api.test.FakeRequest
import play.api.test.Helpers.{route, status, contentAsString, GET, OK, defaultAwaitTimeout}
import org.scalatest.matchers.must.Matchers

class RegistrationConfirmationControllerSpec
    extends AnyWordSpec
    with SpecBase
    with ScalaCheckPropertyChecks
    with Matchers {

  private val primaryEmail   = "test@example.com"
  private val secondaryEmail = "secondary@example.com"
  private val utrNumber      = "1234567890"
  private val ninoNumber     = "AA123456A"

  private def setupUserAnswers(
      emailPage: QuestionPage[String],
      secondaryEmailOpt: Option[String] = None,
      idOpt: Option[(QuestionPage[String], String)] = None
  ): UserAnswers = {
    val base = emptyUserAnswers
      .set(emailPage, primaryEmail)
      .success
      .value

    val withSecondary =
      secondaryEmailOpt.fold(base)(email => base.set(OrganisationSecondContactEmailPage, email).success.value)

    idOpt.fold(withSecondary) { case (idPage, idVal) =>
      withSecondary.set(idPage, idVal).success.value
    }
  }

  private val UTRPage: QuestionPage[String] = UniqueTaxpayerReferenceInUserAnswers

  private val journeys = Seq(
    "OrgWithUtr"   -> Some((UTRPage, utrNumber)),
    "OrgWithoutId" -> None,
    "IndWithNino"  -> Some((NiNumberPage, ninoNumber)),
    "IndWithUtr"   -> Some((UTRPage, utrNumber)),
    "IndWithoutId" -> None
  )

  journeys.foreach { case (journeyName, idOpt) =>
    s"GET /registration-confirmation for $journeyName" should {
      s"return 200 and render the registration confirmation page" in {
        val emailPage = journeyName.startsWith("Org") match {
          case true  => FirstContactEmailPage
          case false => IndividualEmailPage
        }

        val userAnswers = setupUserAnswers(emailPage, secondaryEmailOpt = Some(secondaryEmail), idOpt = idOpt)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        val request = FakeRequest(GET, routes.RegistrationConfirmationController.onPageLoad().url)

        val result = route(application, request).value

        status(result)     mustEqual OK
        contentAsString(result) must include("Registration Confirmation")

        application.stop()
      }
    }
  }
}
