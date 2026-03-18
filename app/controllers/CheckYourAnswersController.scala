/*
 * Copyright 2025 HM Revenue & Customs
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

import cats.implicits.*
import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, SubmissionLockAction}
import models.JourneyType.{IndWithNino, IndWithUtr, IndWithoutId, OrgWithUtr, OrgWithoutId}
import models.error.ApiError
import models.error.ApiError.AlreadyRegisteredError
import models.requests.DataRequest
import models.{JourneyType, NormalMode, SafeId, SubscriptionId, UserAnswers}
import navigation.Navigator
import pages.orgWithoutId.OrganisationBusinessAddressPage
import pages.{NavigatorOnlyCheckYourAnswersErrors, WhereDoYouLivePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{EnrolmentService, RegistrationService, SubscriptionService}
import types.ResultT
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CheckYourAnswersHelper
import viewmodels.Section
import views.html.CheckYourAnswersView

import scala.concurrent.ExecutionContext

class CheckYourAnswersController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    navigator: Navigator,
    val controllerComponents: MessagesControllerComponents,
    helper: CheckYourAnswersHelper,
    submissionLock: SubmissionLockAction,
    registrationService: RegistrationService,
    subscriptionService: SubscriptionService,
    enrolmentService: EnrolmentService,
    view: CheckYourAnswersView,
    sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with Logging
    with I18nSupport {

  private def businessDetailsSectionMaybe(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] =
    helper.getBusinessDetailsSectionMaybe(userAnswers)

  private def orgWithoutIdDetailsMaybe(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] =
    helper.getOrgWithoutIdDetailsMaybe(userAnswers)

  private def firstContactDetailsSectionMaybe(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] =
    helper.getFirstContactDetailsSectionMaybe(userAnswers)

  private def secondContactDetailsSectionMaybe(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] =
    helper.getSecondContactDetailsSectionMaybe(userAnswers)

  private def indWithNinoYourDetails(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] =
    helper.indWithNinoYourDetailsMaybe(userAnswers)

  private def indWithoutIdYourDetails(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] =
    helper.indWithoutIdYourDetailsMaybe(userAnswers)

  private def indContactDetails(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] =
    helper.indContactDetailsMaybe(userAnswers)

  def onPageLoad(): Action[AnyContent] = (identify() andThen getData() andThen submissionLock andThen requireData) {
    implicit request =>
      val userAnswers                      = request.userAnswers
      val journeyType: Option[JourneyType] = userAnswers.journeyType

      val sectionsMaybe = journeyType match {
        case Some(OrgWithUtr)   =>
          for {
            section1 <- businessDetailsSectionMaybe(userAnswers)
            section2 <- firstContactDetailsSectionMaybe(userAnswers)
            section3 <- secondContactDetailsSectionMaybe(userAnswers)
          } yield Seq(section1, section2, section3)
        case Some(IndWithNino)  =>
          for {
            section1 <- indWithNinoYourDetails(userAnswers)
            section2 <- indContactDetails(userAnswers)
          } yield Seq(section1, section2)
        case Some(IndWithUtr)   =>
          for {
            section1 <- businessDetailsSectionMaybe(userAnswers)
            section2 <- indContactDetails(userAnswers)
          } yield Seq(section1, section2)
        case Some(OrgWithoutId) =>
          for {
            section1 <- orgWithoutIdDetailsMaybe(userAnswers)
            section2 <- firstContactDetailsSectionMaybe(userAnswers)
            section3 <- secondContactDetailsSectionMaybe(userAnswers)
          } yield Seq(section1, section2, section3)
        case Some(IndWithoutId) =>
          for {
            section1 <- indWithoutIdYourDetails(userAnswers)
            section2 <- indContactDetails(userAnswers)
          } yield Seq(section1, section2)
        case _                  =>
          logger.warn(s"[CheckYourAnswersController] Error! Journey Type was missing from user answers")
          None
      }
      sectionsMaybe.fold(Redirect(controllers.routes.InformationMissingController.onPageLoad())) { sections =>
        Ok(view(sections))
      }
  }

  def onSubmit(): Action[AnyContent] = (identify() andThen getData() andThen submissionLock andThen requireData)
    .async { implicit request =>
      subscribeAndEnrol().value.map {
        case Right(result)                => result
        case Left(AlreadyRegisteredError) =>
          Redirect(navigator.nextPage(NavigatorOnlyCheckYourAnswersErrors, NormalMode, request.userAnswers))
        case error                        =>
          logger.error(s"[CheckYourAnswersController] Failed to subscribe: $error")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }

  private def subscribeAndEnrol()(implicit request: DataRequest[AnyContent], ec: ExecutionContext) = {
    lazy val answers = request.userAnswers
    for {
      safeId               <- registrationService.getSafeId(request.userAnswers)
      userAnswersWithSafeId = request.userAnswers.copy(safeId = Some(safeId))
      subscriptionId       <- subscriptionService.subscribe(userAnswersWithSafeId)
      journeyTypeMaybe      = request.userAnswers.journeyType
      _                    <- enrolmentCall(answers, subscriptionId)
      result               <- ResultT.fromFuture {
                                val updatedUserAnswers = userAnswersWithSafeId.copy(subscriptionId = Some(subscriptionId))
                                sessionRepository.set(updatedUserAnswers).map { _ =>
                                  Right[ApiError, Result](
                                    Redirect(controllers.routes.RegistrationConfirmationController.onPageLoad())
                                  )
                                }
                              }
    } yield result
  }

  private def enrolmentCall(userAnswers: UserAnswers, subscriptionId: SubscriptionId)(implicit
      hc: HeaderCarrier
  ): ResultT[Unit] =
    userAnswers.journeyType.fold(ResultT.fromValue(())) {
      case journeyType @ (OrgWithUtr | IndWithUtr) =>
        val postcodeMaybe = helper.getUserPostcode(journeyType, userAnswers)
        enrolmentService.enrol(subscriptionId, postcodeMaybe, isAbroad = false)
      case journeyType @ IndWithNino               => enrolmentService.enrol(subscriptionId, None, isAbroad = false)
      case journeyType @ OrgWithoutId              =>
        val postcodeMaybe = helper.getUserPostcode(journeyType, userAnswers)
        enrolmentService.enrol(subscriptionId, postcodeMaybe, isAbroad = true)
      case journeyType @ IndWithoutId              =>
        userAnswers.get(WhereDoYouLivePage).fold(ResultT.fromError[Unit](ApiError.InternalServerError)) { inUk =>
          val postcodeMaybe = helper.getUserPostcode(journeyType, userAnswers)
          enrolmentService.enrol(subscriptionId, postcodeMaybe, isAbroad = !inUk)
        }
    }
}
