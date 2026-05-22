package pages.orgWithoutId

import base.SpecBase

class HaveTradingNamePageSpec extends SpecBase {

  "HaveTradingNamePage" - {
    "cleanup" - {
      "must remove trading name when answer changes to no" in {
        val ua = emptyUserAnswers.withPage(TradingNamePage, "Trading Name")

        val result = HaveTradingNamePage.cleanup(false, ua, hasChanged = true).success.value

        result.get(TradingNamePage) mustBe None
      }

      "must keep trading name when answer changes to yes" in {
        val ua = emptyUserAnswers.withPage(TradingNamePage, "Trading Name")

        val result = HaveTradingNamePage.cleanup(true, ua, hasChanged = true).success.value

        result.get(TradingNamePage) mustBe Some("Trading Name")
      }

      "must keep trading name when answer has not changed" in {
        val ua = emptyUserAnswers.withPage(TradingNamePage, "Trading Name")

        val result = HaveTradingNamePage.cleanup(false, ua, hasChanged = false).success.value

        result.get(TradingNamePage) mustBe Some("Trading Name")
      }
    }
  }
}
