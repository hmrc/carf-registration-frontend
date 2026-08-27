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

package repositories

import config.{CryptoProvider, FrontendAppConfig}
import models.CryptoType.{randomAesKey, CryptoT}
import models.JourneyType.OrgWithUtr
import models.responses.*
import models.{CryptoType, SafeId, SubscriptionId, UserAnswers}
import org.mockito.Mockito.when
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters
import org.scalactic.source.Position
import org.scalatest.OptionValues
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.MDC
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.crypto.Crypted
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.play.bootstrap.dispatchers.MDCPropagatingExecutorService

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, LocalDate, ZoneId}
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

class SessionRepositorySpec
    extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[UserAnswers]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar
    with Eventually {

  private val instant          = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val testSafeId: SafeId                 = SafeId("XE0000123456789")
  private val testSubscriptionId: SubscriptionId = SubscriptionId("CARF0000000001")
  private val testEmail                          = "hi@example.com"
  private val testPhone                          = "07123456789"

  private val userAnswers = UserAnswers(
    id = "id",
    data = Json.obj("foo" -> "bar"),
    journeyType = Some(OrgWithUtr),
    lastUpdated = Instant.ofEpochSecond(1),
    safeId = Some(testSafeId),
    subscriptionId = Some(testSubscriptionId),
    displaySubscriptionResponse = Some(testIndividualDisplaySubscriptionResponse(false))
  )

  def testIndividualDisplaySubscriptionResponse(hasPhone: Boolean) = DisplaySubscriptionResponse(success =
    DisplaySubscriptionSuccess(
      processingDate = LocalDate.now().toString,
      carfSubscriptionDetails = DisplaySubscriptionDetails(
        carfReference = testSubscriptionId.value,
        tradingName = Some("testTradingName"),
        gbUser = true,
        primaryContact = DisplaySubscriptionContact(
          individual =
            Some(DisplaySubscriptionIndividual(firstName = "Timmy", middleName = Some("Tim"), lastName = "Timothy")),
          organisation = None,
          email = testEmail,
          phone = if (hasPhone) Some(testPhone) else None,
          mobile = None
        ),
        secondaryContact = None
      )
    )
  )

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1L
  when(mockAppConfig.mongoEncryptionEnabled) thenReturn true

  private implicit val crypto: CryptoT = new CryptoProvider(Configuration("crypto.key" -> randomAesKey)).get()

  protected override val repository: SessionRepository = new SessionRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    clock = stubClock
  )(scala.concurrent.ExecutionContext.Implicits.global)

  ".set" - {

    "must set the last updated time on the supplied user answers to `now`, and save them" in {

      val expectedResult = userAnswers copy (lastUpdated = instant)

      repository.set(userAnswers).futureValue
      val updatedRecord = find(Filters.equal("_id", userAnswers.id)).futureValue.headOption.value

      updatedRecord mustEqual expectedResult
    }

    "must persist the data in encrypted format" in {
      val setResult = repository.set(userAnswers).futureValue

      setResult mustEqual true

      val retrievedRecord = repository.collection
        .find[BsonDocument](Filters.and(Filters.equal("_id", userAnswers.id)))
        .headOption()
        .futureValue
        .value

      val rawData                        = retrievedRecord.get("data").asString().getValue
      val rawSafeId                      = retrievedRecord.get("safeId").asString().getValue
      val rawSubscriptionId              = retrievedRecord.get("subscriptionId").asString().getValue
      val rawDisplaySubscriptionResponse = retrievedRecord.get("displaySubscriptionResponse").asString().getValue

      val decryptedData                        = crypto.decrypt(Crypted(rawData)).value
      val decryptedSafeId                      = crypto.decrypt(Crypted(rawSafeId)).value
      val decryptedSubscriptionId              = crypto.decrypt(Crypted(rawSubscriptionId)).value
      val decryptedDisplaySubscriptionResponse = crypto.decrypt(Crypted(rawDisplaySubscriptionResponse)).value

      Json.parse(decryptedData)                        mustBe userAnswers.data
      Json.parse(decryptedSafeId)                      mustBe Json.toJson(testSafeId)
      Json.parse(decryptedSubscriptionId)              mustBe Json.toJson(testSubscriptionId)
      Json.parse(decryptedDisplaySubscriptionResponse) mustBe Json.toJson(
        testIndividualDisplaySubscriptionResponse(false)
      )
    }

    mustPreserveMdc(repository.set(userAnswers))
  }

  ".get" - {

    "when there is a record for this id" - {

      "must update the lastUpdated time and get the record" in {

        val collection = repository.collection
        collection.insertOne(userAnswers).toFuture().futureValue

        val result         = repository.get(userAnswers.id).futureValue
        val expectedResult = userAnswers.copy(lastUpdated = instant)

        result.value mustEqual expectedResult
      }
    }

    "when there is no record for this id" - {

      "must return None" in {

        repository.get("id that does not exist").futureValue must not be defined
      }
    }

    mustPreserveMdc(repository.get(userAnswers.id))
  }

  ".clear" - {

    "must remove a record" in {

      insert(userAnswers).futureValue

      repository.clear(userAnswers.id).futureValue

      repository.get(userAnswers.id).futureValue must not be defined
    }

    "must return true when there is no record to remove" in {
      val result = repository.clear("id that does not exist").futureValue

      result mustEqual true
    }

    mustPreserveMdc(repository.clear(userAnswers.id))
  }

  ".keepAlive" - {

    "when there is a record for this id" - {

      "must update its lastUpdated to `now` and return true" in {

        insert(userAnswers).futureValue

        repository.keepAlive(userAnswers.id).futureValue

        val expectedUpdatedAnswers = userAnswers copy (lastUpdated = instant)

        val updatedAnswers = find(Filters.equal("_id", userAnswers.id)).futureValue.headOption.value
        updatedAnswers mustEqual expectedUpdatedAnswers
      }
    }

    "when there is no record for this id" - {

      "must return true" in {

        repository.keepAlive("id that does not exist").futureValue mustEqual true
      }
    }

    mustPreserveMdc(repository.keepAlive(userAnswers.id))
  }

  private def mustPreserveMdc[A](f: => Future[A])(using pos: Position): Unit =
    "must preserve MDC" in {

      implicit lazy val ec: ExecutionContext =
        ExecutionContext.fromExecutor(new MDCPropagatingExecutorService(Executors.newFixedThreadPool(2)))

      MDC.put("test", "foo")

      eventually {
        f.map { _ =>
          MDC.get("test") mustEqual "foo"
        }.futureValue
      }
    }
}
