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
import models.DataRequestWithSubscriptionId
import models.error.{ApiError, CarfError, DataError}
import models.responses.hasIndividualChangedData
import pages.changeContactDetails.{ChangeDetailsIndividualEmailPage, ChangeDetailsIndividualHavePhonePage, ChangeDetailsIndividualPhoneNumberPage}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.SubscriptionService
import types.ResultT
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ChangeIndividualDetailsHelper
import views.html.ChangeIndividualContactDetailsView

import scala.concurrent.{ExecutionContext, Future}

class ChangeIndividualContactDetailsController @Inject() (
    val controllerComponents: MessagesControllerComponents,
    carfIdRetrieval: CarfIdRetrievalAction,
    changeDetailsDataRequiredAction: ChangeDetailsDataRequiredAction,
    subscriptionService: SubscriptionService,
    changeDetailsHelper: ChangeIndividualDetailsHelper,
    view: ChangeIndividualContactDetailsView,
    sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (carfIdRetrieval() andThen changeDetailsDataRequiredAction).async {
    implicit request =>

      val backToManageLink =
        routes.PlaceholderController.onPageLoad("Must redirect to service home page (CARF-411)").url

      request.userAnswers.displaySubscriptionResponse.fold(
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
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
            Future.successful(Ok(view(summaryListRows, hasChanged, backToManageLink)))
          case None                                =>
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
      updateSub().value.map {
        case Right(result)   => result
        case Left(DataError) =>
          logger.error(s"[ChangeIndividualContactDetailsController] Had missing data on submission")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case error           =>
          logger.error(s"[ChangeIndividualContactDetailsController] Failed to update: $error")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }

  private def updateSub()(implicit request: DataRequestWithSubscriptionId[AnyContent], ec: ExecutionContext) = {
    lazy val answers = request.userAnswers
    for {
      subscriptionId <- subscriptionService.updateSubscription(answers)
      result         <- ResultT.fromFuture {
                          val updatedUserAnswers = answers.copy(subscriptionId = Some(subscriptionId))
                          sessionRepository.set(updatedUserAnswers).map { _ =>
                            Right[ApiError, Result](
                              Redirect(controllers.changeContactDetails.routes.ChangeDetailsUpdatedController.onPageLoad())
                            )

                          }
                        }
    } yield result
  }
}
