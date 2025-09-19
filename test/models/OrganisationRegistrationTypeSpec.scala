package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class OrganisationRegistrationTypeSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "OrganisationRegistrationType" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(OrganisationRegistrationType.values.toSeq)

      forAll(gen) {
        organisationRegistrationType =>

          JsString(organisationRegistrationType.toString).validate[OrganisationRegistrationType].asOpt.value mustEqual organisationRegistrationType
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!OrganisationRegistrationType.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[OrganisationRegistrationType] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(OrganisationRegistrationType.values.toSeq)

      forAll(gen) {
        organisationRegistrationType =>

          Json.toJson(organisationRegistrationType) mustEqual JsString(organisationRegistrationType.toString)
      }
    }
  }
}
