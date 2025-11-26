package forms

import forms.behaviours.OptionFieldBehaviours
import models.SecondContactHavePhone
import play.api.data.FormError

class SecondContactHavePhoneFormProviderSpec extends OptionFieldBehaviours {

  val form = new SecondContactHavePhoneFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "secondContactHavePhone.error.required"

    behave like optionsField[SecondContactHavePhone](
      form,
      fieldName,
      validValues  = SecondContactHavePhone.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
