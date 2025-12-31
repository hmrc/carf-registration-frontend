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

sealed trait IndividualRegistrationType:
  def value: RegistrationType

object IndividualRegistrationType {
  final case class SoleTrader(value: RegistrationType = RegistrationType.SoleTrader) extends IndividualRegistrationType

  final case class Individual(value: RegistrationType = RegistrationType.Individual) extends IndividualRegistrationType

  def fromRegistrationType(registrationType: RegistrationType): Option[IndividualRegistrationType] =
    registrationType match
      case RegistrationType.SoleTrader => Some(SoleTrader())
      case RegistrationType.Individual => Some(Individual())
      case _                           => None

  implicit val writes: Writes[IndividualRegistrationType] =
    Writes(ij => JsString(ij.value.toString))

  val values: Seq[IndividualRegistrationType] = Seq(
    SoleTrader(),
    Individual()
  )

  // TODO: add tests or make ticket for adding tests
  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map { case (value, index) =>
    RadioItem(
      content = Text(messages(s"individualRegistrationType.${value.value.messagesKey}")),
      value = Some(value.toString),
      id = Some(s"value_$index")
    )
  }

  implicit val enumerable: Enumerable[IndividualRegistrationType] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
