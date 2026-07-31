/*
 * Copyright 2026 HM Revenue & Customs
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

package models.crypto

import models.{SafeId, SubscriptionId}
import models.responses.DisplaySubscriptionResponse
import play.api.libs.json.JsObject
import uk.gov.hmrc.crypto.Sensitive

case class SensitiveJsObject(override val decryptedValue: JsObject) extends Sensitive[JsObject]

case class SensitiveSubscriptionId(override val decryptedValue: SubscriptionId) extends Sensitive[SubscriptionId]

case class SensitiveSafeId(override val decryptedValue: SafeId) extends Sensitive[SafeId]

case class SensitiveResponse(override val decryptedValue: DisplaySubscriptionResponse)
    extends Sensitive[DisplaySubscriptionResponse]
