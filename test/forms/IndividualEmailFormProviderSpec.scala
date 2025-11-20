package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class IndividualEmailFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "individualEmail.error.required"
  val lengthKey = "individualEmail.error.length"
  val maxLength = 132

  val form = new IndividualEmailFormProvider()()

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
