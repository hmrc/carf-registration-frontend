package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class NiNumberFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "niNumber.error.required"
  val lengthKey   = "niNumber.error.length"
  val maxLength   = 9

  val form = new NiNumberFormProvider()()

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
