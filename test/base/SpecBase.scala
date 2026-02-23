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

package base

import config.Constants.ukTimeZoneStringId
import controllers.actions.*
import generators.Generators
import models.countries.{Country, CountryUk}
import models.responses.{AddressRecord, AddressRegistrationResponse, AddressResponse, CountryRecord}
import models.{AddressUk, BusinessDetails, UniqueTaxpayerReference, UserAnswers}
import org.mockito.Mockito.reset
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, EitherValues, OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{BodyParsers, PlayBodyParsers}
import play.api.test.FakeRequest
import queries.Settable
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext

trait SpecBase
    extends AnyFreeSpec
    with GuiceOneAppPerSuite
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with EitherValues
    with IntegrationPatience
    with BeforeAndAfterEach
    with MockitoSugar
    with Generators {

  val userAnswersId: String            = "id"
  val testUtr: UniqueTaxpayerReference = UniqueTaxpayerReference("1234567890")
  val testUtrString: String            = testUtr.uniqueTaxPayerReference
  val testInternalId: String           = "12345"

  private val UtcZoneId          = "UTC"
  implicit val fixedClock: Clock = Clock.fixed(Instant.parse("2020-05-20T12:34:56.789012Z"), ZoneId.of(UtcZoneId))

  def emptyUserAnswers: UserAnswers = UserAnswers(id = userAnswersId, lastUpdated = Instant.now(fixedClock))

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  def injectedParsers: PlayBodyParsers = app.injector.instanceOf[PlayBodyParsers]

  final val mockSessionRepository: SessionRepository       = mock[SessionRepository]
  final val mockDataRetrievalAction: DataRetrievalAction   = mock[DataRetrievalAction]
  final val mockCtUtrRetrievalAction: CtUtrRetrievalAction = mock[CtUtrRetrievalAction]

  override def beforeEach(): Unit = {
    reset(mockSessionRepository, mockDataRetrievalAction, mockCtUtrRetrievalAction)
    super.beforeEach()
  }
  protected def applicationBuilder(
      userAnswers: Option[UserAnswers] = None,
      affinityGroup: AffinityGroup = AffinityGroup.Individual,
      requestUtr: Option[String] = None
  ): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].toInstance(new FakeIdentifierAction(injectedParsers, affinityGroup)),
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalActionProvider(userAnswers, requestUtr)),
        bind[SubmissionLockAction].to[FakeSubmissionLockAction],
        bind[SessionRepository].toInstance(mockSessionRepository)
      )

  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  implicit class UserAnswersExtension(userAnswers: UserAnswers) {

    def withPage[T](page: Settable[T], value: T)(implicit writes: Writes[T]): UserAnswers =
      userAnswers.set(page, value).success.value

    def withoutPage[T](page: Settable[T])(implicit writes: Writes[T]): UserAnswers =
      userAnswers.remove(page).success.value

  }

  val clock: Clock = Clock.fixed(Instant.ofEpochMilli(1718118467838L), ZoneId.of(ukTimeZoneStringId))

  lazy val testPostcode: String = validPostcodes.sample.value

  def oneAddressResponse: AddressResponse =
    AddressResponse(
      id = "123",
      address = AddressRecord(
        lines = List("1 Test", "Test Street", "Test Region"),
        town = "Testingtown",
        postcode = testPostcode,
        country = CountryRecord(code = "GB", name = "United Kingdom")
      )
    )

  lazy val testAddressUk: AddressUk = AddressUk(
    addressLine1 = "1 Test",
    addressLine2 = Some("Test Street"),
    addressLine3 = Some("Test Region"),
    townOrCity = "Testingtown",
    postCode = testPostcode,
    countryUk = CountryUk("GB", "United Kingdom")
  )

  lazy val multipleAddressResponses: Seq[AddressResponse] =
    Seq(oneAddressResponse, oneAddressResponse, oneAddressResponse)

  val testSignOutUrl: String       = "http://localhost:9553/bas-gateway/sign-out-without-state"
  val testLoginContinueUrl: String = "http://localhost:17000/register-for-carf"

  val testAddressRegistrationResponse = AddressRegistrationResponse(
    addressLine1 = "2 High Street",
    addressLine2 = Some("Birmingham"),
    addressLine3 = None,
    addressLine4 = None,
    postalCode = Some("B23 2AZ"),
    countryCode = "GB"
  )

  val testBusinessDetails = BusinessDetails(name = "TestName", address = testAddressRegistrationResponse)

}
