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

package controllers.changeContactDetails

import cats.syntax.all.*
import com.google.inject.Inject
import controllers.actions.{CarfIdRetrievalAction, ChangeDetailsDataRequiredAction}
import controllers.routes
import models.error.ApiError.ApplicationError
import models.error.DataError
import pages.changeContactDetails.{ChangeDetailsIndividualEmailPage, ChangeDetailsIndividualHavePhonePage}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SubscriptionService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ChangeDetailsHelper
import views.html.ChangeIndividualContactDetailsView

import scala.concurrent.{ExecutionContext, Future}

class ChangeIndividualContactDetailsController @Inject() (
    val controllerComponents: MessagesControllerComponents,
    carfIdRetrieval: CarfIdRetrievalAction,
    changeDetailsDataRequiredAction: ChangeDetailsDataRequiredAction,
    subscriptionService: SubscriptionService,
    changeDetailsHelper: ChangeDetailsHelper,
    view: ChangeIndividualContactDetailsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (carfIdRetrieval() andThen changeDetailsDataRequiredAction).async {
    implicit request =>

      val backToManageLink =
        routes.PlaceholderController.onPageLoad("Must redirect to service home page (CARF-411)").url

      val userAnswers = request.userAnswers

      val maybeSummaryListRows = changeDetailsHelper.getFirstContactDetailsSectionMaybe(userAnswers)
      val maybeEmail           = userAnswers.get(ChangeDetailsIndividualEmailPage)
      val maybeHavePhone       = userAnswers.get(ChangeDetailsIndividualHavePhonePage)

      val eitherHasChanged = changeDetailsHelper.getHasChanged(
        maybeEmail,
        maybeHavePhone,
        userAnswers
      )

      lazy val redirectOnMissingDetails = changeDetailsHelper
        .decideContinueUrl(maybeEmail, maybeHavePhone, userAnswers)
        .fold(
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        ) { url =>
          Future.successful(
            Redirect(
              controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad(url)
            )
          )
        }

      eitherHasChanged match {
        case Right(hasChanged) =>
          maybeSummaryListRows match {
            case Some(summaryListRows) => Future.successful(Ok(view(summaryListRows, hasChanged, backToManageLink)))
            case None                  => redirectOnMissingDetails
          }
        case Left(DataError)   => redirectOnMissingDetails
        case Left(_)           => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  def onSubmit(): Action[AnyContent] = (carfIdRetrieval() andThen changeDetailsDataRequiredAction).async {
    implicit request =>
      subscriptionService.updateSubscription(request.subscriptionId) map {
        case Left(value) =>
          logger.warn(s"[ChangeIndividualContactDetailsController] Error updating user details")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Right(())   =>
          Redirect(
            controllers.changeContactDetails.routes.ChangeDetailsUpdatedController.onPageLoad()
          )
      }
  }
}
