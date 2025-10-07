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
import models.{Business, IsThisYourBusinessPageDetails, Mode, UniqueTaxpayerReference}
import navigation.Navigator
import pages.{IndexPage, IsThisYourBusinessPage, YourUniqueTaxpayerReferencePage}
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
          businessService.getBusinessByUtr(utr.uniqueTaxPayerReference).flatMap {
            case Some(business) =>
              val existingPageDetails = request.userAnswers.get(IsThisYourBusinessPage)
              val pageDetails         = IsThisYourBusinessPageDetails(
                name = business.name,
                address = business.address,
                pageAnswer = existingPageDetails.flatMap(_.pageAnswer)
              )

              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(IsThisYourBusinessPage, pageDetails))
                _              <- sessionRepository.set(updatedAnswers)
              } yield {
                val preparedForm = pageDetails.pageAnswer match {
                  case None        => form
                  case Some(value) => form.fill(value)
                }
                logger.info(s"Fresh business data cached for UTR: ${utr.uniqueTaxPayerReference}")
                Ok(view(preparedForm, mode, business))
              }

            case None =>
              logger.warn(
                s"Business not found for UTR. Redirecting to journey recovery."
              )
              Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
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

  private def processFormSubmission(
      existingPageDetails: IsThisYourBusinessPageDetails,
      mode: Mode
  )(implicit request: DataRequest[AnyContent]): Future[Result] = {

    val business = Business(existingPageDetails.name, existingPageDetails.address)

    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          logger.debug("Form submission contained errors")
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
