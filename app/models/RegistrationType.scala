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
import play.api.libs.json.*
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

enum RegistrationType(val code: String, val messagesKey: String):
  case LimitedCompany extends RegistrationType("0000", "limitedCompany")
  case Partnership extends RegistrationType("0001", "partnership")
  case LLP extends RegistrationType("0002", "llp")
  case Trust extends RegistrationType("0003", "trust")
  case SoleTrader extends RegistrationType("0004", "soleTrader")
  case Individual extends RegistrationType("Individual code not needed", "individual")

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

  implicit val reads: Reads[OrganisationRegistrationType] =
    RegistrationType.reads.flatMap { registrationType =>
      Reads { _ =>
        fromRegistrationType(registrationType)
          .map(JsSuccess(_))
          .getOrElse(
            JsError(s"$registrationType is not a valid OrganisationRegistrationType")
          )
      }
    }

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

  implicit val reads: Reads[IndividualRegistrationType] =
    RegistrationType.reads.flatMap { registrationType =>
      Reads { _ =>
        fromRegistrationType(registrationType)
          .map(JsSuccess(_))
          .getOrElse(
            JsError(s"$registrationType is not a valid IndividualRegistrationType")
          )
      }
    }

  implicit val writes: Writes[IndividualRegistrationType] =
    Writes(ij => JsString(ij.value.toString))

  val values: Seq[IndividualRegistrationType] = Seq(
    SoleTrader(),
    Individual()
  )

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

object RegistrationType {
  implicit val reads: Reads[RegistrationType] = Reads {
    case JsString(v) =>
      RegistrationType.values
        .find(_.toString == v)
        .map(JsSuccess(_))
        .getOrElse(JsError(s"Unknown RegistrationType: $v"))

    case _ =>
      JsError("RegistrationType must be a string")
  }

  implicit val writes: Writes[RegistrationType] =
    Writes(registrationType => JsString(registrationType.toString))

}
