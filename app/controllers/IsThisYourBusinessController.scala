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
import models.{BusinessDetails, IsThisYourBusinessPageDetails, Mode, UniqueTaxpayerReference}
import navigation.Navigator
import pages.{IndexPage, IsThisYourBusinessPage, WhatIsTheNameOfYourBusinessPage, YourUniqueTaxpayerReferencePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
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
          handleBusinessLookup(
            businessService.getBusinessWithUserInput(request.userAnswers),
            userInputUtr.uniqueTaxPayerReference,
            mode,
            isAutoMatch = false
          )
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
    lookupFuture.flatMap {
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
          val preparedForm = pageDetails.pageAnswer.fold(form)(form.fill)
          logger.info(s"Business data found and cached for UTR: $utr.")
          Ok(view(preparedForm, mode, business))
        }

      case None =>
        val journeyType = if (isAutoMatch) "Auto-Match" else "Manual-Entry"
        logger.warn(s"IsThisYourBusinessController: Business not found. Journey type: $journeyType.")

        if (isAutoMatch) {
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        } else {
          Future.successful(
            Redirect(
              routes.PlaceholderController.onPageLoad("Must redirect to /problem/business-not-identified (CARF-147)")
            )
          )
        }
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
