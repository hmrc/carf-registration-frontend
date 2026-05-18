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

import models.*
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import models.countries.*
import models.responses.AddressRegistrationResponse

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

  implicit lazy val arbitraryOrganisationRegistrationType: Arbitrary[OrganisationRegistrationType] =
    Arbitrary {
      Gen.oneOf(OrganisationRegistrationType.values)
    }

  implicit lazy val arbitraryIndividualRegistrationType: Arbitrary[IndividualRegistrationType] =
    Arbitrary {
      Gen.oneOf(IndividualRegistrationType.values)
    }

  implicit lazy val arbitraryIndFindAddress: Arbitrary[IndFindAddress] =
    Arbitrary {
      for {
        postcode             <- arbitrary[String]
        propertyNameOrNumber <- arbitrary[String]
      } yield IndFindAddress(postcode, Some(propertyNameOrNumber))
    }

  implicit lazy val arbitraryCountryUk: Arbitrary[CountryUk] =
    Arbitrary {
      for {
        code <- arbitrary[String]
        name <- arbitrary[String]
      } yield CountryUk(code, name)
    }

  implicit lazy val arbitraryAddressUk: Arbitrary[AddressUk] =
    Arbitrary {
      for {
        addressLine1 <- addressStringGen
        addressLine2 <- Gen.option(addressStringGen)
        addressLine3 <- Gen.option(addressStringGen)
        townOrCity   <- arbitrary[String]
        postcode     <- postcodeStringGen
        countryUk    <- arbitrary[CountryUk]
      } yield AddressUk(
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        addressLine3 = addressLine3,
        townOrCity = townOrCity,
        postCode = postcode,
        countryUk = countryUk
      )
    }

  implicit lazy val arbitraryUniqueTaxpayerReference: Arbitrary[UniqueTaxpayerReference] =
    Arbitrary {
      for {
        utr <- addressStringGen
      } yield UniqueTaxpayerReference(utr)
    }

  implicit lazy val arbitraryAddressRegistrationResponse: Arbitrary[AddressRegistrationResponse] =
    Arbitrary {
      for {
        addressLine1 <- addressStringGen
        addressLine2 <- Gen.option(addressStringGen)
        addressLine3 <- Gen.option(addressStringGen)
        addressLine4 <- Gen.option(addressStringGen)
        postalCode   <- Gen.option(postcodeStringGen)
        countryCode  <- arbitrary[String]
        countryName  <- Gen.option(arbitrary[String])
      } yield AddressRegistrationResponse(
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        addressLine3 = addressLine3,
        addressLine4 = addressLine4,
        postalCode = postalCode,
        countryCode = countryCode,
        countryName = countryName
      )
    }

  implicit lazy val arbitraryBusinessDetails: Arbitrary[BusinessDetails] =
    Arbitrary {
      for {
        name    <- arbitrary[String]
        address <- arbitrary[AddressRegistrationResponse]
        safeId  <- arbitrary[String]
      } yield BusinessDetails(name, address, safeId)
    }

  implicit lazy val arbitraryIsThisYourBusinessPageDetails: Arbitrary[IsThisYourBusinessPageDetails] =
    Arbitrary {
      for {
        businessDetails <- arbitrary[BusinessDetails]
        pageAnswer      <- Gen.option(arbitrary[Boolean])
      } yield IsThisYourBusinessPageDetails(businessDetails, pageAnswer)
    }
}
