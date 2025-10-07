/*
 * Copyright 2025 HM Revenue & Customs
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
import models.{Business, Mode, UniqueTaxpayerReference}
import navigation.Navigator
import pages.{BusinessDetailsPage, IndexPage, IsThisYourBusinessPage, YourUniqueTaxpayerReferencePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.RegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
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
    with Logging {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>

      val utrOpt = request.userAnswers
        .get(YourUniqueTaxpayerReferencePage)
        .orElse(request.userAnswers.get(IndexPage))

      utrOpt match {
        case Some(utr) =>
          request.userAnswers.get(BusinessDetailsPage) match {
            case Some(business) =>
              val preparedForm = request.userAnswers.get(IsThisYourBusinessPage) match {
                case None        => form
                case Some(value) => form.fill(value)
              }
              Future.successful(Ok(view(preparedForm, mode, business)))

            case None =>
              fetchAndCacheBusiness(utr, mode)
          }

        case None =>
          logger.warn(
            "No UTR found in user answers (YourUniqueTaxpayerReferencePage or IndexPage). Redirecting to journey recovery."
          )
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>

      val utrOpt = request.userAnswers
        .get(YourUniqueTaxpayerReferencePage)
        .orElse(request.userAnswers.get(IndexPage))

      utrOpt match {
        case Some(utr) =>
          request.userAnswers.get(BusinessDetailsPage) match {
            case Some(business) =>
              processFormSubmission(business, utr, mode)
            case None           =>
              businessService.getBusinessByUtr(utr.uniqueTaxPayerReference).flatMap {
                case Some(business) =>
                  processFormSubmission(business, utr, mode)
                case None           =>
                  logger.warn(
                    s"Business not found for UTR: ${utr.uniqueTaxPayerReference} during form submission. Redirecting to journey recovery."
                  )
                  Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
              }
          }

        case None =>
          logger.warn(
            "No UTR found in user answers during form submission (neither YourUniqueTaxpayerReferencePage nor IndexPage). " +
              "Redirecting to journey recovery."
          )
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  private def fetchAndCacheBusiness(utr: UniqueTaxpayerReference, mode: Mode)(implicit
      request: DataRequest[AnyContent]
  ): Future[Result] =
    businessService.getBusinessByUtr(utr.uniqueTaxPayerReference).flatMap {
      case Some(business) =>
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(BusinessDetailsPage, business))
          _              <- sessionRepository.set(updatedAnswers)
        } yield {
          val preparedForm = updatedAnswers.get(IsThisYourBusinessPage) match {
            case None        => form
            case Some(value) => form.fill(value)
          }
          Ok(view(preparedForm, mode, business))
        }

      case None =>
        logger.warn(
          s"Business not found for UTR: ${utr.uniqueTaxPayerReference}. Redirecting to journey recovery."
        )
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }

  private def processFormSubmission(business: Business, utr: UniqueTaxpayerReference, mode: Mode)(implicit
      request: DataRequest[AnyContent]
  ): Future[Result] =
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, business))),
        value =>
          for {
            answersWithFormValue <- Future.fromTry(request.userAnswers.set(IsThisYourBusinessPage, value))
            updatedAnswers       <- Future.fromTry(answersWithFormValue.set(BusinessDetailsPage, business))
            _                    <- sessionRepository.set(updatedAnswers)
          } yield {
            logger
              .info(s"User answered '$value' for IsThisYourBusiness with UTR: ${utr.uniqueTaxPayerReference}")
            logger.info(s"Business details cached for UTR: ${utr.uniqueTaxPayerReference}")
            Redirect(navigator.nextPage(IsThisYourBusinessPage, mode, updatedAnswers))
          }
      )
}
