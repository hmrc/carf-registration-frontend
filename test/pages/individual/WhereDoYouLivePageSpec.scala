package pages.individual

import base.SpecBase
import models.countries.Country
import models.{AddressAndUPRN, IndFindAddress, IndWithoutIdAddressNonUk, UserAnswers}
import pages.individualWithoutId.*

class WhereDoYouLivePageSpec extends SpecBase {

  private val userAnswersAllPages: UserAnswers = emptyUserAnswers
    .withPage(IndFindAddressAdditionalCallUa, false)
    .withPage(IndFindAddressPage, IndFindAddress(testPostcode, None))
    .withPage(AddressLookupPage, Seq(AddressAndUPRN(testAddressUk, testUPRN)))
    .withPage(AddressUPRNUserAnswers, testUPRN)
    .withPage(IndWithoutIdAddressPagePrePop, testAddressUk)
    .withPage(IndWithoutIdChooseAddressPage, testAddressUk.addressLine1)
    .withPage(IndWithoutIdSelectedChooseAddressPage, testAddressUk)
    .withPage(IndWithoutIdUkAddressInUserAnswers, testAddressUk)
    .withPage(IndWithoutIdAddressNonUkPage, IndWithoutIdAddressNonUk(
      "123 Main Street",
      Some("Apt 4"),
      "Paris",
      Some("Ile-de-France"),
      Some("75001"),
      Country("FR", "France")
    ))
  
  "WhereDoYouLivePage" - {
    "cleanup" - {
      "must clear UK pages when answer has changed and answer is false (No)" in {
        
        val result = WhereDoYouLivePage.cleanup(false, userAnswersAllPages, true).get

        result.get(IndFindAddressAdditionalCallUa) mustBe None
        result.get(IndFindAddressPage) mustBe None
        result.get(AddressLookupPage) mustBe None
        result.get(AddressUPRNUserAnswers) mustBe None
        result.get(IndWithoutIdAddressPagePrePop) mustBe None
        result.get(IndWithoutIdChooseAddressPage) mustBe None
        result.get(IndWithoutIdSelectedChooseAddressPage) mustBe None
        result.get(IndWithoutIdUkAddressInUserAnswers) mustBe None
        result.get(IndWithoutIdAddressNonUkPage).isDefined mustBe true
      }
      
      "must not clear UK pages when answer has NOT changed and answer is true (irrelevant)" in {
        
        val result = WhereDoYouLivePage.cleanup(true, userAnswersAllPages, false).get

        result.get(IndFindAddressAdditionalCallUa).isDefined mustBe true
        result.get(IndFindAddressPage).isDefined mustBe true
        result.get(AddressLookupPage).isDefined mustBe true
        result.get(AddressUPRNUserAnswers).isDefined mustBe true
        result.get(IndWithoutIdAddressPagePrePop).isDefined mustBe true
        result.get(IndWithoutIdChooseAddressPage).isDefined mustBe true
        result.get(IndWithoutIdSelectedChooseAddressPage).isDefined mustBe true
        result.get(IndWithoutIdUkAddressInUserAnswers).isDefined mustBe true
        result.get(IndWithoutIdAddressNonUkPage).isDefined mustBe true
      }

      "must clear non-UK pages when answer has changed and answer is false (No)" in {

        val result = WhereDoYouLivePage.cleanup(true, userAnswersAllPages, true).get

        result.get(IndFindAddressAdditionalCallUa).isDefined mustBe true
        result.get(IndFindAddressPage).isDefined mustBe true
        result.get(AddressLookupPage).isDefined mustBe true
        result.get(AddressUPRNUserAnswers).isDefined mustBe true
        result.get(IndWithoutIdAddressPagePrePop).isDefined mustBe true
        result.get(IndWithoutIdChooseAddressPage).isDefined mustBe true
        result.get(IndWithoutIdSelectedChooseAddressPage).isDefined mustBe true
        result.get(IndWithoutIdUkAddressInUserAnswers).isDefined mustBe true
        result.get(IndWithoutIdAddressNonUkPage) mustBe None
      }
    }
  }
}
