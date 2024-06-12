/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentmappingfrontend.repository

import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions, ReplaceOptions}
import play.api.Logging
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.agentmappingfrontend.model.MongoLocalDateTimeFormat
import uk.gov.hmrc.agentmappingfrontend.repository.MappingResult.MappingArnResultId
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import java.time.temporal.ChronoUnit.MILLIS

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class ClientCountAndGGTag(clientCount: Int, ggTag: String)

case object ClientCountAndGGTag {
  implicit val formats: OFormat[ClientCountAndGGTag] = Json.format
}

case class MappingArnResult(
  id: MappingArnResultId,
  arn: Arn,
  createdDate: LocalDateTime = Instant.now().atZone(ZoneOffset.UTC).toLocalDateTime.truncatedTo(MILLIS),
  currentCount: Int,
  currentGGTag: String = "",
  clientCountAndGGTags: Seq[ClientCountAndGGTag] = Seq.empty,
  alreadyMapped: Boolean = false
)

object MappingResult {
  type MappingArnResultId = String
}

object MappingArnResult {

  def apply(arn: Arn, currentCount: Int, clientCountAndGGTags: Seq[ClientCountAndGGTag]): MappingArnResult = {
    val id: MappingArnResultId = UUID.randomUUID().toString.replace("-", "")
    MappingArnResult(id = id, arn = arn, currentCount = currentCount, clientCountAndGGTags = clientCountAndGGTags)
  }

  implicit val localDateTimeFormat: Format[LocalDateTime] = MongoLocalDateTimeFormat.localDateTimeFormat
  implicit val format: OFormat[MappingArnResult] = Json.format
}

@Singleton
class MappingArnRepository @Inject() (mongoComponent: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[MappingArnResult](
      mongoComponent = mongoComponent,
      collectionName = "mapping-arn",
      domainFormat = MappingArnResult.format,
      indexes = Seq(
        IndexModel(ascending("id"), IndexOptions().name("idUnique").unique(true)),
        IndexModel(
          ascending("createdDate"),
          IndexOptions().name("createDate").unique(false).expireAfter(86400, TimeUnit.SECONDS)
        )
      ),
      replaceIndexes = true
    ) with Logging {

  def create(
    arn: Arn,
    currentCount: Int = 0,
    clientCountAndGGTags: Seq[ClientCountAndGGTag] = Seq.empty
  ): Future[MappingArnResultId] = {
    val record = MappingArnResult(arn = arn, currentCount = currentCount, clientCountAndGGTags = clientCountAndGGTags)
    collection
      .insertOne(record)
      .toFuture()
      .map(_ => record.id)
  }

  def findRecord(id: MappingArnResultId): Future[Option[MappingArnResult]] =
    collection
      .find(equal("id", id))
      .headOption()

  def upsert(mappingArnResult: MappingArnResult, id: MappingArnResultId): Future[Unit] =
    collection
      .replaceOne(
        equal("id", id),
        mappingArnResult,
        ReplaceOptions()
          .upsert(true)
      )
      .toFuture()
      .map(wr =>
        if (!wr.wasAcknowledged()) throw new RuntimeException("Something went wrong with upsert.")
        else
          logger.info(
            s"Upsert success. Found ${wr.getMatchedCount} matching documents. " +
              s"${wr.getModifiedCount} were modified."
          )
      )

  def updateCurrentGGTag(id: MappingArnResultId, ggTag: String): Future[Unit] =
    collection
      .updateOne(equal("id", id), set("currentGGTag", ggTag))
      .toFuture()
      .map(wr =>
        if (!wr.wasAcknowledged()) throw new RuntimeException("Something went wrong with updateCurrentGGTag.")
        else
          logger.info(
            s"updateCurrentGGTag success. Found ${wr.getMatchedCount} matching documents. " +
              s"${wr.getModifiedCount} were modified."
          )
      )

  def updateMappingCompleteStatus(id: MappingArnResultId): Future[Unit] =
    collection
      .updateOne(equal("id", id), set("alreadyMapped", true))
      .toFuture()
      .map(wr =>
        if (!wr.wasAcknowledged())
          throw new RuntimeException("Something went wrong with updateMappingCompleteStatus.")
        else
          logger.info(
            s"updateMappingCompleteStatus success. Found ${wr.getMatchedCount} matching documents. " +
              s"${wr.getModifiedCount} were modified."
          )
      )

  def delete(id: MappingArnResultId): Future[Unit] =
    collection
      .deleteOne(equal("id", id))
      .toFuture()
      .map(_ => ())
}
