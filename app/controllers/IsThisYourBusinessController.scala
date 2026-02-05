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

import controllers.actions.*
import forms.IsThisYourBusinessFormProvider
import models.JourneyType.IndWithUtr
import models.RegistrationType.SoleTrader
import models.error.ApiError.NotFoundError
import models.error.{ApiError, CarfError}
import models.requests.DataRequest
import models.{BusinessDetails, IndividualDetails, IsThisYourBusinessPageDetails, Mode, UniqueTaxpayerReference}
import navigation.Navigator
import pages.organisation.UniqueTaxpayerReferenceInUserAnswers
import pages.IsThisYourBusinessPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.RegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.UserAnswersHelper
import views.html.IsThisYourBusinessView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IsThisYourBusinessController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: IsThisYourBusinessFormProvider,
    businessService: RegistrationService,
    val controllerComponents: MessagesControllerComponents,
    view: IsThisYourBusinessView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with UserAnswersHelper
    with Logging {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      val maybeUtr                   = request.userAnswers.get(UniqueTaxpayerReferenceInUserAnswers)
      val maybeJourneyTypeSoleTrader = request.userAnswers.journeyType.map(_ == IndWithUtr)
      val isAutoMatched: Boolean     = request.userAnswers.isCtAutoMatched

      (maybeJourneyTypeSoleTrader, maybeUtr) match {
        case (Some(false), Some(utr))         =>
          handleBusinessLookup(
            businessService.getBusinessWithUtr(request.userAnswers, utr.uniqueTaxPayerReference),
            utr.uniqueTaxPayerReference,
            mode,
            isAutoMatch = isAutoMatched
          )
        case (Some(true), Some(userInputUtr)) =>
          handleIndividualLookup(
            businessService.getIndividualByUtr(request.userAnswers),
            userInputUtr.uniqueTaxPayerReference,
            mode
          )

        case (_, _) =>
          logger.warn(
            s"No UTR or no JourneyType <$maybeJourneyTypeSoleTrader> found in user answers. Redirecting to journey recovery."
          )
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      request.userAnswers.get(IsThisYourBusinessPage) match {
        case Some(existingPageDetails) =>
          val business =
            BusinessDetails(existingPageDetails.businessDetails.name, existingPageDetails.businessDetails.address)
          form
            .bindFromRequest()
            .fold(
              formWithErrors => {
                logger.warn("Form submission contained errors")
                Future.successful(BadRequest(view(formWithErrors, mode, business)))
              },
              value => {
                val updatedPageDetails = existingPageDetails.copy(pageAnswer = Some(value))
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(IsThisYourBusinessPage, updatedPageDetails))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield {
                  logger.info(s"User answered '$value' for IsThisYourBusiness with business")
                  Redirect(navigator.nextPage(IsThisYourBusinessPage, mode, updatedAnswers))
                }
              }
            )
        case None                      =>
          logger.warn(
            "No existing details for IsThisYourBusinessPage found in UserAnswers on form submission. Redirecting to journey recovery."
          )
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  private def handleBusinessLookup(
      lookupFuture: Future[Either[CarfError, BusinessDetails]],
      utr: String,
      mode: Mode,
      isAutoMatch: Boolean
  )(implicit request: DataRequest[AnyContent]): Future[Result] =
    lookupFuture.flatMap {
      case Right(business) =>
        val existingPageDetails = request.userAnswers.get(IsThisYourBusinessPage)

        val pageDetails = IsThisYourBusinessPageDetails(
          businessDetails = BusinessDetails(name = business.name, address = business.address),
          pageAnswer = existingPageDetails.flatMap(_.pageAnswer)
        )

        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(IsThisYourBusinessPage, pageDetails))
          _              <- sessionRepository.set(updatedAnswers)
        } yield {
          val preparedForm = existingPageDetails.flatMap(_.pageAnswer).fold(form)(form.fill)
          logger.info(s"Business data found and cached for UTR: $utr.")
          Ok(view(preparedForm, mode, business))
        }

      case Left(NotFoundError) =>
        if (isSoleTrader(request.userAnswers)) {
          logger.warn("User is a Sole Trader. Redirecting to sole-trader-not-identified.")
          Future.successful(
            Redirect(controllers.individual.routes.ProblemSoleTraderNotIdentifiedController.onPageLoad())
          )
        } else if (isAutoMatch) {
          logger.warn("Auto-match failed for a non-Sole Trader. Redirecting to journey recovery.")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        } else {
          logger.warn("Manual entry failed for a non-Sole Trader. Redirecting to business-not-identified.")
          Future.successful(Redirect(controllers.organisation.routes.BusinessNotIdentifiedController.onPageLoad()))
        }
      case Left(error)         =>
        logger.warn(s"Unexpected error. Error: $error")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }

  private def handleIndividualLookup(
      lookupFuture: Future[Either[CarfError, IndividualDetails]],
      utr: String,
      mode: Mode
  )(implicit request: DataRequest[AnyContent]): Future[Result] =
    lookupFuture.flatMap {
      case Right(individualDetails) =>
        val soleTraderBusinessDetails = BusinessDetails(individualDetails.fullName, individualDetails.address)

        val pageDetails = IsThisYourBusinessPageDetails(
          businessDetails = soleTraderBusinessDetails,
          pageAnswer = request.userAnswers.get(IsThisYourBusinessPage).flatMap(_.pageAnswer)
        )

        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(IsThisYourBusinessPage, pageDetails))
          _              <- sessionRepository.set(updatedAnswers)
        } yield {
          val preparedForm = pageDetails.pageAnswer.fold(form)(form.fill)
          logger.info(s"Sole Trader Business data found and cached for UTR: $utr.")
          Ok(view(preparedForm, mode, soleTraderBusinessDetails))
        }
      case Left(NotFoundError)      =>
        logger.warn("User is a Sole Trader. Redirecting to sole-trader-not-identified.")
        Future.successful(Redirect(controllers.individual.routes.ProblemSoleTraderNotIdentifiedController.onPageLoad()))
      case Left(error)              =>
        logger.warn(s"Unexpected error. Error: $error")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
}
