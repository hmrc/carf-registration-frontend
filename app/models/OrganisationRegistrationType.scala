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
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait OrganisationRegistrationType {
  val code: String
}

object OrganisationRegistrationType extends Enumerable.Implicits {

  case object LimitedCompany extends WithName("limitedCompany") with OrganisationRegistrationType {
    override val code: String = "0000"
  }
  case object Partnership extends WithName("partnership") with OrganisationRegistrationType {
    override val code: String = "0001"

  }
  case object LLP extends WithName("llp") with OrganisationRegistrationType {
    override val code: String = "0002"

  }
  case object Trust extends WithName("trust") with OrganisationRegistrationType {
    override val code: String = "0003"

  }
  case object SoleTrader extends WithName("soleTrader") with OrganisationRegistrationType {
    override val code: String = "0004"

  }

  val values: Seq[OrganisationRegistrationType] = Seq(
    LimitedCompany,
    Partnership,
    LLP,
    Trust,
    SoleTrader
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map { case (value, index) =>
    RadioItem(
      content = Text(messages(s"organisationRegistrationType.${value.toString}")),
      value = Some(value.toString),
      id = Some(s"value_$index")
    )
  }

  implicit val enumerable: Enumerable[OrganisationRegistrationType] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
