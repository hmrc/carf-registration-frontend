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

import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsObject, JsValue, Json}
class OrgWithoutIdBusinessNameSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {
  "OrgWithoutIdBusinessName" - {
    "must deserialise any string value" in {
      forAll(arbitrary[String]) { str =>
        val jsVal: JsValue = Json.obj("value" -> str)
        jsVal.validate[OrgWithoutIdBusinessName].asOpt.value mustEqual OrgWithoutIdBusinessName(str)
      }
    }
    "must serialise any string value" in {
      forAll(arbitrary[String]) { str =>
        val obj = OrgWithoutIdBusinessName(str)
        Json.toJson(obj) mustEqual Json.obj("value" -> str)
      }
    }
  }
}
