package forms

import forms.behaviours.OptionFieldBehaviours
import models.IndividualRegistrationType
import play.api.data.FormError

class IndividualRegistrationTypeFormProviderSpec extends OptionFieldBehaviours {

  val form = new IndividualRegistrationTypeFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "individualRegistrationType.error.required"

    behave like optionsField[IndividualRegistrationType](
      form,
      fieldName,
      validValues  = IndividualRegistrationType.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
