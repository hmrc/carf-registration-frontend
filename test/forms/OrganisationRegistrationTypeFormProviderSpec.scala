package forms

import forms.behaviours.OptionFieldBehaviours
import models.OrganisationRegistrationType
import play.api.data.FormError

class OrganisationRegistrationTypeFormProviderSpec extends OptionFieldBehaviours {

  val form = new OrganisationRegistrationTypeFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "organisationRegistrationType.error.required"

    behave like optionsField[OrganisationRegistrationType](
      form,
      fieldName,
      validValues  = OrganisationRegistrationType.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
