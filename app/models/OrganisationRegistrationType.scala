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

sealed trait OrganisationRegistrationType:
  def value: RegistrationType

object OrganisationRegistrationType {
  final case class LimitedCompany(value: RegistrationType = RegistrationType.LimitedCompany)
      extends OrganisationRegistrationType

  final case class Partnership(value: RegistrationType = RegistrationType.Partnership)
      extends OrganisationRegistrationType

  final case class LLP(value: RegistrationType = RegistrationType.LLP) extends OrganisationRegistrationType

  final case class Trust(value: RegistrationType = RegistrationType.Trust) extends OrganisationRegistrationType

  final case class SoleTrader(value: RegistrationType = RegistrationType.SoleTrader)
      extends OrganisationRegistrationType

  def fromRegistrationType(registrationType: RegistrationType): Option[OrganisationRegistrationType] =
    registrationType match
      case RegistrationType.LimitedCompany => Some(LimitedCompany())
      case RegistrationType.Partnership    => Some(Partnership())
      case RegistrationType.LLP            => Some(LLP())
      case RegistrationType.Trust          => Some(Trust())
      case RegistrationType.SoleTrader     => Some(SoleTrader())
      case _                               => None

  implicit val writes: Writes[OrganisationRegistrationType] =
    Writes(oj => JsString(oj.value.toString))

  val values: Seq[OrganisationRegistrationType] = Seq(
    LimitedCompany(),
    Partnership(),
    LLP(),
    Trust(),
    SoleTrader()
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map { case (value, index) =>
    RadioItem(
      content = Text(messages(s"organisationRegistrationType.${value.value.messagesKey}")),
      value = Some(value.toString),
      id = Some(s"value_$index")
    )
  }

  implicit val enumerable: Enumerable[OrganisationRegistrationType] =
    Enumerable(values.map(v => v.toString -> v): _*)

}
