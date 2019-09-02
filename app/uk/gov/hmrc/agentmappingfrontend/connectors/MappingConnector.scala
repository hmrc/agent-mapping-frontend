/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.JsValue
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.metrics.Metrics
import uk.gov.hmrc.agentmappingfrontend.model.{MappingDetailsRequest, SaMapping, VatMapping}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MappingConnector @Inject()(
                                  http: HttpClient,
                                  metrics: Metrics,
                                  appConfig: AppConfig)  {

  def createMapping(arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Int] = {
    val timerContext = metrics.createMapping.time()
    http.PUT(createUrl(arn), "")
        .map {
          timerContext.stop()
          r =>
          r.status
        }
        .recover {
          case e: Upstream4xxResponse if Status.FORBIDDEN.equals(e.upstreamResponseCode) => Status.FORBIDDEN
          case e: Upstream4xxResponse if Status.CONFLICT.equals(e.upstreamResponseCode)  => Status.CONFLICT
          case e                                                                         => throw e
        }
    }

  def getClientCount(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Int] = {
    val timerContext = metrics.getClientCount.time()
    http.GET[HttpResponse](createUrlClientCount)
        .map {
          timerContext.stop()
          response => (response.json \ "clientCount").as[Int]
        }
    }

  def findSaMappingsFor(arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[SaMapping]] = {
    val timerContext = metrics.findSaMappingsFor.time()
    http.GET[JsValue](findSaUrl(arn)).map {
      response =>
        timerContext.stop()
        (response \ "mappings").as[Seq[SaMapping]]
    } recover {
      case _: NotFoundException => Seq.empty
      case ex: Throwable => throw new RuntimeException(ex)
    }
  }

  def findVatMappingsFor(arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[VatMapping]] = {
    val timerContext = metrics.findVatMappingsFor.time()
    http.GET[JsValue](findVatUrl(arn)).map { response =>
      timerContext.stop()
      (response \ "mappings").as[Seq[VatMapping]]
    }recover {
        case _: NotFoundException => Seq.empty
        case ex: Throwable        => throw new RuntimeException(ex)
      }
    }

  def deleteAllMappingsBy(arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Int] = {
    val timeContext = metrics.deleteAllMappingsFor.time()
    http.DELETE(deleteUrl(arn)).map {
      timeContext.stop()
      r =>
        r.status
      }
    }

  def createOrUpdateMappingDetails(arn: Arn, mappingDetailsRequest: MappingDetailsRequest)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext) = {
    val timerContext = metrics.createOrUpdateMappingDetails.time()
      http.POST[MappingDetailsRequest, HttpResponse](createOrUpdateUrl(arn), mappingDetailsRequest).map{
        timerContext.stop()
        r => r.status
    }.recover {
      case ex =>
        Logger.error(s"creating or updating mapping details failed for some reason: $ex")
        throw new RuntimeException
    }
  }

  private lazy val baseUrl = appConfig.agentMappingBaseUrl

  private def createUrlClientCount: String = s"$baseUrl/agent-mapping/client-count"

  private def createUrl(arn: Arn): String = s"$baseUrl/agent-mapping/mappings/arn/${arn.value}"

  private def deleteUrl(arn: Arn): String = s"$baseUrl/agent-mapping/test-only/mappings/${arn.value}"

  private def findSaUrl(arn: Arn): String = s"$baseUrl/agent-mapping/mappings/sa/${arn.value}"

  private def findVatUrl(arn: Arn): String = s"$baseUrl/agent-mapping/mappings/vat/${arn.value}"

  private def createOrUpdateUrl(arn: Arn): String = s"$baseUrl/agent-mapping/mappings/details/arn/${arn.value}"

}
