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
import models.requests.DataRequest
import models.{BusinessDetails, IndividualDetails, IsThisYourBusinessPageDetails, Mode, UniqueTaxpayerReference}
import navigation.Navigator
import pages.organisation.YourUniqueTaxpayerReferencePage
import pages.{IndexPage, IsThisYourBusinessPage}
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
      val maybeIndexPage   = request.userAnswers.get(IndexPage)
      val maybeYourUtrPage = request.userAnswers.get(YourUniqueTaxpayerReferencePage)

      (maybeIndexPage, maybeYourUtrPage) match {
        case (Some(autoMatchUtr), _) =>
          handleBusinessLookup(
            businessService.getBusinessWithEnrolmentCtUtr(autoMatchUtr.uniqueTaxPayerReference),
            autoMatchUtr.uniqueTaxPayerReference,
            mode,
            isAutoMatch = true
          )
        case (_, Some(userInputUtr)) =>
          if (isSoleTrader(request.userAnswers)) {
            handleIndividualLookup(
              businessService.getIndividualByUtr(request.userAnswers),
              userInputUtr.uniqueTaxPayerReference,
              mode,
              isAutoMatch = false
            )
          } else {
            handleBusinessLookup(
              businessService.getBusinessWithUserInput(request.userAnswers),
              userInputUtr.uniqueTaxPayerReference,
              mode,
              isAutoMatch = false
            )
          }
        case (_, _)                  =>
          logger.warn("No UTR found in user answers. Redirecting to journey recovery.")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      request.userAnswers.get(IsThisYourBusinessPage) match {
        case Some(existingPageDetails) =>
          processFormSubmission(existingPageDetails, mode)

        case None =>
          logger.warn(
            "No business details found in UserAnswers during form submission. " + "Redirecting to journey recovery."
          )
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  private def handleBusinessLookup(
      lookupFuture: Future[Option[BusinessDetails]],
      utr: String,
      mode: Mode,
      isAutoMatch: Boolean
  )(implicit request: DataRequest[AnyContent]): Future[Result] =
    lookupFuture
      .flatMap {
        case Some(business) =>
          val existingPageDetails = request.userAnswers.get(IsThisYourBusinessPage)

          val pageDetails = IsThisYourBusinessPageDetails(
            name = business.name,
            address = business.address,
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

        case None =>
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
      }

  private def handleIndividualLookup(
      lookupFuture: Future[Option[IndividualDetails]],
      utr: String,
      mode: Mode,
      isAutoMatch: Boolean
  )(implicit request: DataRequest[AnyContent]): Future[Result] =
    lookupFuture.flatMap {
      case Some(individualDetails) =>
        val existingPageDetails    = request.userAnswers.get(IsThisYourBusinessPage)
        val soleTraderBusinessName = s"${individualDetails.firstName} ${individualDetails.lastName}"
        val address                = individualDetails.address
        val pageAnswer             = existingPageDetails.flatMap(_.pageAnswer)

        val pageDetails               = IsThisYourBusinessPageDetails(soleTraderBusinessName, address, pageAnswer)
        val soleTraderBusinessDetails = BusinessDetails(soleTraderBusinessName, address)

        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(IsThisYourBusinessPage, pageDetails))
          _              <- sessionRepository.set(updatedAnswers)
        } yield {
          val preparedForm = pageDetails.pageAnswer.fold(form)(form.fill)
          logger.info(s"Sole Trader Business data found and cached for UTR: $utr.")
          Ok(view(preparedForm, mode, soleTraderBusinessDetails))
        }
      case None                    =>
        logger.warn("User is a Sole Trader. Redirecting to sole-trader-not-identified.")
        Future.successful(Redirect(controllers.individual.routes.ProblemSoleTraderNotIdentifiedController.onPageLoad()))
    }

  private def processFormSubmission(
      existingPageDetails: IsThisYourBusinessPageDetails,
      mode: Mode
  )(implicit request: DataRequest[AnyContent]): Future[Result] = {

    val business = BusinessDetails(existingPageDetails.name, existingPageDetails.address)

    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          logger.debug("Is this your business form submission contained errors")
          Future.successful(BadRequest(view(formWithErrors, mode, business)))
        },
        value => {
          val updatedPageDetails = existingPageDetails.copy(pageAnswer = Some(value))

          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(IsThisYourBusinessPage, updatedPageDetails))
            _              <- sessionRepository.set(updatedAnswers)
          } yield {
            logger.info(s"User answered '$value' for IsThisYourBusiness with business: ${existingPageDetails.name}")
            Redirect(navigator.nextPage(IsThisYourBusinessPage, mode, updatedAnswers))
          }
        }
      )
  }
}
