package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class FirstContactNameFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "firstContactName.error.required"
  val lengthKey = "firstContactName.error.length"
  val maxLength = 35

  val form = new FirstContactNameFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
