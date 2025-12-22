package forms.individual

import forms.behaviours.IntFieldBehaviours
import forms.individual.IndividualPhoneNumberFormProvider
import play.api.data.FormError

class IndividualPhoneNumberFormProviderSpec extends IntFieldBehaviours {

  val form = new IndividualPhoneNumberFormProvider()()

  ".value" - {

    val fieldName = "value"

    val minimum = 0
    val maximum = 24

    val validDataGenerator = intsInRangeWithCommas(minimum, maximum)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like intField(
      form,
      fieldName,
      nonNumericError  = FormError(fieldName, "individualPhoneNumber.error.nonNumeric"),
      wholeNumberError = FormError(fieldName, "individualPhoneNumber.error.wholeNumber")
    )

    behave like intFieldWithRange(
      form,
      fieldName,
      minimum       = minimum,
      maximum       = maximum,
      expectedError = FormError(fieldName, "individualPhoneNumber.error.outOfRange", Seq(minimum, maximum))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "individualPhoneNumber.error.required")
    )
  }
}
