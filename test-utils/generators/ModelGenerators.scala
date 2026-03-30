/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package generators

import models._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import models.countries.*

trait ModelGenerators {

  implicit lazy val arbitraryIndWithoutIdAddressNonUk: Arbitrary[IndWithoutIdAddressNonUk] =
    Arbitrary {
      for {
        addressLine1 <- addressStringGen
        addressLine2 <- Gen.option(addressStringGen)
        townOrCity   <- addressStringGen
        region       <- Gen.option(addressStringGen)
        postcode     <- Gen.option(postcodeStringGen)
        country      <- arbitrary[Country]
      } yield IndWithoutIdAddressNonUk(addressLine1, addressLine2, townOrCity, region, postcode, country)
    }

  val addressStringGen: Gen[String] = Gen.alphaNumStr.suchThat(_.nonEmpty).map(_.take(35))

  val postcodeStringGen: Gen[String] = Gen.alphaNumStr.suchThat(_.nonEmpty).map(_.take(10))

  implicit lazy val arbitraryCountry: Arbitrary[Country] =
    Arbitrary {
      for {
        code        <- Gen.pick(2, 'A' to 'Z').map(_.mkString)
        description <- Gen.alphaStr.suchThat(_.nonEmpty)
      } yield Country(code, description)
    }

  implicit lazy val arbitraryOrganisationBusinessAddress: Arbitrary[OrganisationBusinessAddress] =
    Arbitrary {
      for {
        addressLine1 <- addressStringGen
        addressLine2 <- Gen.option(addressStringGen)
        townOrCity   <- addressStringGen
        region       <- Gen.option(addressStringGen)
        postcode     <- Gen.option(postcodeStringGen)
        country      <- arbitrary[Country]
      } yield OrganisationBusinessAddress(addressLine1, addressLine2, townOrCity, region, postcode, country)
    }

  implicit lazy val arbitraryWhatIsYourName: Arbitrary[Name] =
    Arbitrary {
      for {
        firstName <- arbitrary[String]
        lastName  <- arbitrary[String]
      } yield Name(firstName, lastName)
    }

  implicit lazy val arbitraryWhatIsYourNameIndividual: Arbitrary[Name] =
    Arbitrary {
      for {
        firstName <- arbitrary[String]
        lastName  <- arbitrary[String]
      } yield Name(firstName, lastName)
    }

  implicit lazy val arbitraryOrganisationRegistrationType: Arbitrary[OrganisationRegistrationType] =
    Arbitrary {
      Gen.oneOf(OrganisationRegistrationType.values.toSeq)
    }

  implicit lazy val arbitraryIndividualRegistrationType: Arbitrary[IndividualRegistrationType] =
    Arbitrary {
      Gen.oneOf(IndividualRegistrationType.values.toSeq)
    }

  implicit lazy val arbitraryIndFindAddress: Arbitrary[IndFindAddress] =
    Arbitrary {
      for {
        postcode             <- arbitrary[String]
        propertyNameOrNumber <- arbitrary[String]
      } yield IndFindAddress(postcode, Some(propertyNameOrNumber))
    }
}
