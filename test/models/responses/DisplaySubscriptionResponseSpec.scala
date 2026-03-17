package models.responses

import base.SpecBase

class DisplaySubscriptionResponseSpec extends SpecBase {

  "DisplaySubscriptionResponse model" - {
    "isIndividualRegistrationType val" - {
      "must return true when only individual primary contact is present" in {
        val result = testIndividualDisplaySubscriptionResponse(true).isIndividualRegistrationType

        result mustBe Some(true)
      }
      "must return false when only organisation primary contact is present" in {
        val result = testOrganisationDisplaySubscriptionResponse.isIndividualRegistrationType

        result mustBe Some(false)
      }
      "must return None when no primary contact is present" in {
        val result = testInvalidDisplaySubscriptionResponse.isIndividualRegistrationType

        result mustBe None
      }
      "must return None when no both primary contact types are present" in {
        val result = testInvalidDisplaySubscriptionResponseBoth.isIndividualRegistrationType

        result mustBe None
      }
    }
  }

  "DisplaySubscriptionResponse object extension method hasIndividualChangedData" - {
    "must return true if email changed" - {
      val result = testIndividualDisplaySubscriptionResponse(true).hasIndividualChangedData(email = "DIFF EMAIL", phone = Some(testPhone))

      result mustBe true
    }
    "must return true if phone changed" - {
      val result = testIndividualDisplaySubscriptionResponse(true).hasIndividualChangedData(email = testEmail, phone = Some("DIFF PHONE"))

      result mustBe true
    }
    "must return true if phone is None" - {
      val result = testIndividualDisplaySubscriptionResponse(true).hasIndividualChangedData(email = testEmail, phone = None)

      result mustBe true
    }
    "must return true if email and phone changed" - {
      val result = testIndividualDisplaySubscriptionResponse(true).hasIndividualChangedData(email = "DIFF EMAIL", phone = Some("DIFF PHONE"))

      result mustBe true
    }
    "must return false if nothing changed" - {
      val result = testIndividualDisplaySubscriptionResponse(true).hasIndividualChangedData(email = testEmail, phone = Some(testPhone))

      result mustBe false
    }
  }

}
