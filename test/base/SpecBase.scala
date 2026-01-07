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
import connectors.AddressLookupConnector
import controllers.actions.*
import models.{UniqueTaxpayerReference, UserAnswers}
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
import queries.Settable
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.PlayBodyParsers
import play.api.test.FakeRequest
import repositories.SessionRepository
import play.api.libs.json.{Json, Writes}
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
    with MockitoSugar {

  val userAnswersId: String            = "id"
  val testUtr: UniqueTaxpayerReference = UniqueTaxpayerReference("1234567890")
  val testInternalId: String           = "12345"

  private val UtcZoneId          = "UTC"
  implicit val fixedClock: Clock = Clock.fixed(Instant.parse("2020-05-20T12:34:56.789012Z"), ZoneId.of(UtcZoneId))

  def emptyUserAnswers: UserAnswers =
    UserAnswers(id = userAnswersId, lastUpdated = Instant.now(fixedClock))

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  def injectedParsers: PlayBodyParsers = app.injector.instanceOf[PlayBodyParsers]

  final val mockSessionRepository: SessionRepository       = mock[SessionRepository]
  final val mockDataRetrievalAction: DataRetrievalAction   = mock[DataRetrievalAction]
  final val mockCtUtrRetrievalAction: CtUtrRetrievalAction = mock[CtUtrRetrievalAction]
  final val mockAddressLookupConnector                     = mock[AddressLookupConnector]

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
        bind[SessionRepository].toInstance(mockSessionRepository)
      )

  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  implicit class UserAnswersExtension(userAnswers: UserAnswers) {

    def withPage[T](page: Settable[T], value: T)(implicit writes: Writes[T]): UserAnswers =
      userAnswers.set(page, value).success.value

  }

  val clock: Clock = Clock.fixed(Instant.ofEpochMilli(1718118467838L), ZoneId.of(ukTimeZoneStringId))
}
