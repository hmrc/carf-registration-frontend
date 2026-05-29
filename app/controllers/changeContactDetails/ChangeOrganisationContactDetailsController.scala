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

import com.google.inject.Inject
import controllers.actions.{CarfIdRetrievalAction, ChangeDetailsDataRequiredAction}
import controllers.routes
import models.error.DataError
import models.responses.hasOrganisationChangedData
import pages.changeContactDetails.*
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SubscriptionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ChangeOrganisationDetailsHelper
import views.html.ChangeOrganisationContactDetailsView

import scala.concurrent.{ExecutionContext, Future}

class ChangeOrganisationContactDetailsController @Inject() (
    val controllerComponents: MessagesControllerComponents,
    carfIdRetrieval: CarfIdRetrievalAction,
    changeDetailsDataRequiredAction: ChangeDetailsDataRequiredAction,
    subscriptionService: SubscriptionService,
    changeDetailsHelper: ChangeOrganisationDetailsHelper,
    view: ChangeOrganisationContactDetailsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (carfIdRetrieval() andThen changeDetailsDataRequiredAction).async {
    implicit request =>

      val backToManageLink =
        routes.PlaceholderController.onPageLoad("Must redirect to service home page (CARF-411)").url

      val userAnswers = request.userAnswers

      userAnswers.displaySubscriptionResponse.fold(
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      ) { displaySubscriptionResponse =>
        val pageDetails = for {
          maybeSummaryListRows       <- changeDetailsHelper.getFirstContactDetailsSectionMaybe(userAnswers)
          firstName                  <- userAnswers.get(ChangeDetailsOrgFirstNamePage)
          firstEmail                 <- userAnswers.get(ChangeDetailsOrgFirstEmailPage)
          firstHavePhone             <- userAnswers.get(ChangeDetailsOrgFirstHavePhonePage)
          firstPhone                 <-
            if (firstHavePhone) {
              userAnswers.get(ChangeDetailsOrgFirstPhoneNumberPage).map(Some(_))
            } else {
              Some(None)
            }
          maybeSecondSummaryListRows <- changeDetailsHelper.getSecondContactDetailsSectionMaybe(userAnswers)
          haveSecond                 <- userAnswers.get(ChangeDetailsOrgHaveSecondContactPage)
          secondContactName          <-
            if (haveSecond) {
              userAnswers.get(ChangeDetailsOrgSecondNamePage).map(Some(_))
            } else {
              Some(None)
            }
          secondEmail                <-
            if (haveSecond) userAnswers.get(ChangeDetailsOrgSecondEmailPage).map(Some(_)) else Some(None)
          secondHavePhone            <-
            if (haveSecond) userAnswers.get(ChangeDetailsOrgSecondHavePhonePage) else Some(false)
          secondPhone                <- if (haveSecond && secondHavePhone)
                                          userAnswers.get(ChangeDetailsOrgSecondPhoneNumberPage).map(Some(_))
                                        else Some(None)
        } yield {
          val hasChanged = displaySubscriptionResponse.hasOrganisationChangedData(
            firstContactEmail = firstEmail,
            firstContactName = firstName,
            firstContactPhone = firstPhone,
            secondContactName = secondContactName,
            secondContactEmail = secondEmail,
            secondContactPhone = secondPhone
          )

          (maybeSummaryListRows, maybeSecondSummaryListRows, hasChanged)
        }

        pageDetails match {
          case Some((summaryListRows, secondSummaryListRows, hasChanged)) =>
            Future.successful(Ok(view(summaryListRows, secondSummaryListRows, hasChanged, backToManageLink)))
          case None                                                       =>
            Future.successful(
              Redirect(
                controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad()
              )
            )
        }

      }
  }

  def onSubmit(): Action[AnyContent] = (carfIdRetrieval() andThen changeDetailsDataRequiredAction).async {
    implicit request =>
      subscriptionService
        .updateSubscription(request.userAnswers, request.subscriptionId.value)
        .value
        .map {
          case Right(value)    =>
            Redirect(controllers.changeContactDetails.routes.ChangeDetailsUpdatedController.onPageLoad())
          case Left(DataError) =>
            logger.error(s"[ChangeOrganisationContactDetailsController] Had missing data on submission")
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          case error           =>
            logger.error(s"[ChangeOrganisationContactDetailsController] Failed to update: $error")
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
  }

}
