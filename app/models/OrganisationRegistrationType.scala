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

package models

import play.api.i18n.Messages
import play.api.libs.json.{JsString, Writes}
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait OrganisationRegistrationType {
  def toRegistrationType: RegistrationType
}
object OrganisationRegistrationType {
  case object OrganisationLimitedCompany extends OrganisationRegistrationType {
    def toRegistrationType: RegistrationType = RegistrationType.LimitedCompany
  }

  case object OrganisationPartnership extends OrganisationRegistrationType {
    def toRegistrationType: RegistrationType = RegistrationType.Partnership
  }

  case object OrganisationLLP extends OrganisationRegistrationType {
    def toRegistrationType: RegistrationType = RegistrationType.LLP
  }

  case object OrganisationTrust extends OrganisationRegistrationType {
    def toRegistrationType: RegistrationType = RegistrationType.Trust
  }

  case object OrganisationSoleTrader extends OrganisationRegistrationType {
    def toRegistrationType: RegistrationType = RegistrationType.SoleTrader
  }

  def fromRegistrationType(registrationType: RegistrationType): Option[OrganisationRegistrationType] =
    registrationType match {
      case RegistrationType.LimitedCompany => Some(OrganisationLimitedCompany)
      case RegistrationType.Partnership    => Some(OrganisationPartnership)
      case RegistrationType.LLP            => Some(OrganisationLLP)
      case RegistrationType.Trust          => Some(OrganisationTrust)
      case RegistrationType.SoleTrader     => Some(OrganisationSoleTrader)
      case _                               => None
    }

  val values: Seq[OrganisationRegistrationType] = Seq(
    OrganisationLimitedCompany,
    OrganisationPartnership,
    OrganisationLLP,
    OrganisationTrust,
    OrganisationSoleTrader
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map { case (value, index) =>
    RadioItem(
      content = Text(messages(s"organisationRegistrationType.${value.toRegistrationType.messagesKey}")),
      value = Some(value.toString),
      id = Some(s"value_$index")
    )
  }

  implicit val enumerable: Enumerable[OrganisationRegistrationType] = Enumerable(values.map(v => v.toString -> v): _*)
}
