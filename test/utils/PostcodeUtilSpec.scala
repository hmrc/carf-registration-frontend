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

package utils

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class PostcodeUtilSpec extends AnyFreeSpec with Matchers {

  "PostcodeUtil.normalise" - {

    "when isCrownDependency is false" - {
      "should trim leading and trailing whitespace" in {
        PostcodeUtil.normalise(isCrownDependency = false, "  AB12 3CD  ") mustEqual "AB12 3CD"
        PostcodeUtil.normalise(isCrownDependency = false, "\tSW1A 1AA\n") mustEqual "SW1A 1AA"
      }

      "should NOT change internal spacing or casing" in {
        PostcodeUtil.normalise(isCrownDependency = false, "ab12   3cd") mustEqual "ab12   3cd"
        PostcodeUtil.normalise(isCrownDependency = false, "sw1a 1aa")   mustEqual "sw1a 1aa"
      }
    }

    "when isCrownDependency is true" - {
      "should remove all input whitespace and convert to uppercase" in {
        PostcodeUtil.normalise(isCrownDependency = true, " j e 1 1 a b ") mustEqual "JE1 1AB"
      }

      "should reformat postcodes longer than 3 characters" - {
        "by inserting a single space before the final 3 characters" in {
          PostcodeUtil.normalise(isCrownDependency = true, "IM13AB") mustEqual "IM1 3AB"
          PostcodeUtil.normalise(isCrownDependency = true, "gy12ab") mustEqual "GY1 2AB"

          PostcodeUtil.normalise(isCrownDependency = true, "JE244CD") mustEqual "JE24 4CD"
        }
      }

      "should handle postcodes with length <= 3" - {
        "by returning the cleaned, uppercase string without adding a space" in {
          PostcodeUtil.normalise(isCrownDependency = true, "IM1")  mustEqual "IM1"
          PostcodeUtil.normalise(isCrownDependency = true, " je ") mustEqual "JE"
          PostcodeUtil.normalise(isCrownDependency = true, "A")    mustEqual "A"
        }
      }

      "should handle complex input with mixed case and irregular spacing" in {
        PostcodeUtil.normalise(isCrownDependency = true, "  im9   9z  z ") mustEqual "IM9 9ZZ"
      }
    }
  }
}
