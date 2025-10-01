package forms

import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError

class HaveNiNumberFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "haveNiNumber.error.required"
  val invalidKey = "error.boolean"

  val form = new HaveNiNumberFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like booleanField(
      form,
      fieldName,
      invalidError = FormError(fieldName, invalidKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
