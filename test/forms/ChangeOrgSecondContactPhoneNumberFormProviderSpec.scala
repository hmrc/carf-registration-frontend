package forms

import forms.behaviours.IntFieldBehaviours
import play.api.data.FormError

class ChangeOrgSecondContactPhoneNumberFormProviderSpec extends IntFieldBehaviours {

  val form = new ChangeOrgSecondContactPhoneNumberFormProvider()()

  ".value" - {

    val fieldName = "value"

    val minimum = 1
    val maximum = 10

    val validDataGenerator = intsInRangeWithCommas(minimum, maximum)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like intField(
      form,
      fieldName,
      nonNumericError  = FormError(fieldName, "changeOrgSecondContactPhoneNumber.error.nonNumeric"),
      wholeNumberError = FormError(fieldName, "changeOrgSecondContactPhoneNumber.error.wholeNumber")
    )

    behave like intFieldWithRange(
      form,
      fieldName,
      minimum       = minimum,
      maximum       = maximum,
      expectedError = FormError(fieldName, "changeOrgSecondContactPhoneNumber.error.outOfRange", Seq(minimum, maximum))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "changeOrgSecondContactPhoneNumber.error.required")
    )
  }
}
