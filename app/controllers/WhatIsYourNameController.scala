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
import forms.WhatIsYourNameFormProvider

import javax.inject.Inject
import models.{Address, IndividualRegistrationType, Mode, Name, OrganisationRegistrationType, UserAnswers}
import navigation.Navigator
import pages.{IndividualRegistrationTypePage, OrganisationRegistrationTypePage, WhatIsYourNamePage, YourUniqueTaxpayerReferencePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.RegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.WhatIsYourNameView

import scala.concurrent.{ExecutionContext, Future}

class WhatIsYourNameController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    registrationService: RegistrationService,
    formProvider: WhatIsYourNameFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: WhatIsYourNameView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Name] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(WhatIsYourNamePage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }
      Ok(view(preparedForm, mode))
  }

  private def isSoleTrader(userAnswers: UserAnswers): Boolean = {

    val individualRegistrationType: Option[IndividualRegistrationType] = userAnswers.get(IndividualRegistrationTypePage)

    val organisationRegistrationType: Option[OrganisationRegistrationType] =
      userAnswers.get(OrganisationRegistrationTypePage)

    (individualRegistrationType, organisationRegistrationType) match {
      case (Some(IndividualRegistrationType.SoleTrader), _) | (_, Some(OrganisationRegistrationType.SoleTrader)) =>
        true
      case _                                                                                                     =>
        false
    }
  }

  private def validateIndividualDetails(answers: UserAnswers, name: Name): Future[Option[(Name, Address)]] =
    answers.get(YourUniqueTaxpayerReferencePage) match {
      case Some(utr) => registrationService.getIndividualDetails(utr.uniqueTaxPayerReference, name)
      case None      => Future.successful(None)
    }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(WhatIsYourNamePage, value))
              _              <- sessionRepository.set(updatedAnswers)
              maybeUtr        = updatedAnswers.get(YourUniqueTaxpayerReferencePage)
              matchResult    <- validateIndividualDetails(updatedAnswers, value)
            } yield matchResult match {
              case Some((matchedName, _)) if matchedName == value =>
                Redirect(navigator.nextPage(WhatIsYourNamePage, mode, updatedAnswers))
              case _                                              =>
                if (isSoleTrader(updatedAnswers)) {
                  Redirect(
                    routes.PlaceholderController
                      .onPageLoad("Must redirect to /problem/sole-trader-not-identified (CARF-129)")
                  )
                } else {
                  Redirect(
                    routes.PlaceholderController
                      .onPageLoad("Must redirect to /problem/business-not-identified (CARF-147)")
                  )
                }
            }
        )
  }
}
