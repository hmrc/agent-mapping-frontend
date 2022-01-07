/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.agentmappingfrontend.connectors

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.model.{MappingDetailsRepositoryRecord, MappingDetailsRequest, SaMapping, VatMapping}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.HttpErrorFunctions._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpClient, _}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MappingConnector @Inject()(http: HttpClient, metrics: Metrics, appConfig: AppConfig)
    extends HttpAPIMonitor with Logging {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  def createMapping(arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Int] =
    monitor("ConsumedAPI-Mapping-CreateMapping-PUT") {
      http
        .PUT[String, HttpResponse](createUrl(arn), "")
        .map(_.status)
    }

  def getClientCount(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Int] =
    monitor("ConsumedAPI-Mapping-ClientCount-GET") {
      http
        .GET[HttpResponse](createUrlClientCount)
        .map { response =>
          (response.json \ "clientCount").as[Int]
        }
    }

  def findSaMappingsFor(arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[SaMapping]] =
    monitor("ConsumedAPI-Mapping-FindSaMappingsForArn-GET") {
      val url = findSaUrl(arn)
      http.GET[HttpResponse](url).flatMap { response =>
        response.status match {
          case OK        => Future((response.json \ "mappings").as[Seq[SaMapping]])
          case NOT_FOUND => Future(Seq.empty)
          case s         => Future.failed(new RuntimeException(s"unexpected error when calling $url, status: $s"))
        }
      }
    }

  def findVatMappingsFor(arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[VatMapping]] =
    monitor("ConsumedAPI-Mapping-FindVatMappingsForArn-GET") {
      val url = findVatUrl(arn)
      http.GET[HttpResponse](url).map { response =>
        response.status match {
          case OK        => (response.json \ "mappings").as[Seq[VatMapping]]
          case NOT_FOUND => Seq.empty
          case s         => throw new RuntimeException(s"unexpected error when calling $url, status: $s")
        }
      }
    }

  def deleteAllMappingsBy(arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Int] =
    monitor("ConsumedAPI-Mapping-DeleteAllMappingsByArn-DELETE") {
      http.DELETE[HttpResponse](deleteUrl(arn)).map(_.status)
    }

  def createOrUpdateMappingDetails(arn: Arn, mappingDetailsRequest: MappingDetailsRequest)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Unit] =
    monitor("ConsumedAPI-Mapping-createOrUpdateMappingDetails-POST") {
      http
        .POST[MappingDetailsRequest, HttpResponse](detailsUrl(arn), mappingDetailsRequest)
        .map { r =>
          r.status match {
            case status if is2xx(status) => ()
            case status if status == CONFLICT =>
              throw new ConflictException(s"Failed to create or update mapping details for $arn")
            case status =>
              logger.error(s"status: $status Failed to create or update mapping details for arn: $arn")
              throw new RuntimeException
          }
        }
    }

  def getMappingDetails(
    arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[MappingDetailsRepositoryRecord]] =
    monitor("ConsumedAPI-Mapping-getMappingDetails-GET") {
      http.GET[Option[MappingDetailsRepositoryRecord]](detailsUrl(arn))
    }.recover {
      case _: NotFoundException =>
        logger.warn(s"no mapping details found for this arn: $arn")
        None
      case ex =>
        logger.warn(s"retrieval of mapping details failed for unknown reason...$ex")
        None
    }

  private lazy val baseUrl = appConfig.agentMappingBaseUrl

  private def createUrlClientCount: String = s"$baseUrl/agent-mapping/client-count"

  private def createUrl(arn: Arn): String = s"$baseUrl/agent-mapping/mappings/arn/${arn.value}"

  private def deleteUrl(arn: Arn): String = s"$baseUrl/agent-mapping/test-only/mappings/${arn.value}"

  private def findSaUrl(arn: Arn): String = s"$baseUrl/agent-mapping/mappings/sa/${arn.value}"

  private def findVatUrl(arn: Arn): String = s"$baseUrl/agent-mapping/mappings/vat/${arn.value}"

  private def detailsUrl(arn: Arn): String = s"$baseUrl/agent-mapping/mappings/details/arn/${arn.value}"

}
