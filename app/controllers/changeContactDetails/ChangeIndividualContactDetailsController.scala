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
import config.FrontendAppConfig
import controllers.actions.{CarfIdRetrievalAction, ChangeDetailsDataRequiredAction}
import models.error.DataError
import models.responses.hasIndividualChangedData
import pages.changeContactDetails.{ChangeDetailsIndividualEmailPage, ChangeDetailsIndividualHavePhonePage, ChangeDetailsIndividualPhoneNumberPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditService, SubscriptionService}
import types.ResultT
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ChangeIndividualDetailsHelper
import utils.LoggerUtil.*
import views.html.ChangeIndividualContactDetailsView

import scala.concurrent.{ExecutionContext, Future}

class ChangeIndividualContactDetailsController @Inject() (
    val controllerComponents: MessagesControllerComponents,
    carfIdRetrieval: CarfIdRetrievalAction,
    changeDetailsDataRequiredAction: ChangeDetailsDataRequiredAction,
    subscriptionService: SubscriptionService,
    changeDetailsHelper: ChangeIndividualDetailsHelper,
    view: ChangeIndividualContactDetailsView,
    appConfig: FrontendAppConfig,
    auditService: AuditService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (carfIdRetrieval() andThen changeDetailsDataRequiredAction) {
    implicit request =>
      request.userAnswers.displaySubscriptionResponse.fold(
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      ) { displaySubscriptionResponse =>

        val pageDetails = for {
          maybeSummaryListRows <- changeDetailsHelper.getFirstContactDetailsSectionMaybe(request.userAnswers)
          email                <- request.userAnswers.get(ChangeDetailsIndividualEmailPage)
          havePhone            <- request.userAnswers.get(ChangeDetailsIndividualHavePhonePage)
          phone                <- if (havePhone) {
                                    request.userAnswers.get(ChangeDetailsIndividualPhoneNumberPage).map(Some(_))
                                  } else {
                                    Some(None)
                                  }
        } yield {
          val hasChanged = displaySubscriptionResponse.hasIndividualChangedData(email, phone)
          (maybeSummaryListRows, hasChanged)
        }

        pageDetails match {
          case Some((summaryListRows, hasChanged)) =>
            Ok(view(summaryListRows, hasChanged, appConfig.carfManagementFrontendHomePageUrl))
          case None                                =>
            Redirect(controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad())
        }
      }
  }

  def onSubmit(): Action[AnyContent] = (carfIdRetrieval() andThen changeDetailsDataRequiredAction).async {
    implicit request =>
      subscriptionService
        .updateSubscription(request.userAnswers, request.subscriptionId.value)
        .value
        .flatMap {
          case Right(value)    =>
            for {
              _ <-
                auditService
                  .auditChangeContactDetails(request.userAnswers, isIndividual = true)
                  .recover { case e =>
                    logDebug(s"Auditing ChangeContactDetails failed due to $e")
                    ()
                  }
                  .value

            } yield Redirect(controllers.changeContactDetails.routes.ChangeDetailsUpdatedController.onPageLoad())
          case Left(DataError) =>
            logError("[ChangeIndividualContactDetailsController] Had missing data on submission")
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          case error           =>
            logError(s"[ChangeIndividualContactDetailsController] Failed to update: $error")
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
  }
}
