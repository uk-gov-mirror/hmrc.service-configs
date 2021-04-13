/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.serviceconfigs.connector

import akka.stream.Materializer
import akka.stream.scaladsl.StreamConverters
import com.google.common.io.BaseEncoding
import play.api.libs.json._
import play.api.libs.ws.{WSAuthScheme, WSClient}
import uk.gov.hmrc.serviceconfigs.config.JenkinsConfig

import java.io.InputStream
import javax.inject.Inject
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{ExecutionContext, Future}



class JenkinsConnector @Inject()(config: JenkinsConfig, ws: WSClient)(implicit ec: ExecutionContext,
materializer: Materializer) {

  def getSensuZip(): Future[InputStream] = {
    ws
      .url(s"${config.orchestratorUrl}/job/seed-service-sensu-alerts/ws/target/output/*zip*/output.zip")
      .withMethod("GET")
      .withAuth(config.username, config.token, WSAuthScheme.BASIC)
      .withRequestTimeout(Duration.Inf)
      .stream
      .map(_.bodyAsSource.async.runWith(StreamConverters.asInputStream(readTimeout = 20.seconds)))
  }

  def getLatestJob(): Future[Option[Int]] = {
    ws
      .url(s"${config.orchestratorUrl}/job/seed-service-sensu-alerts/api/json?depth=1&tree=lastCompletedBuild[number]")
      .withMethod("GET")
      .withAuth(config.username, config.token, WSAuthScheme.BASIC)
      .withRequestTimeout(Duration.Inf)
      .get()
      .map { response => (response.json \ "lastCompletedBuild" \ "number").toOption.map(_.as[Int])
    }
  }
}

//val futureResult: Future[String] = ws.url(url).get().map { response =>
//(response.json \ "person" \ "name").as[String]


  // https://orchestrator.tools.production.tax.service.gov.uk/job/seed-service-sensu-alerts/ws/target/output/*zip*/output.zip


//  @Singleton
//  class GzippedResourceConnector @Inject()(
//                                            ws: WSClient
//                                          )(implicit
//                                            ec          : ExecutionContext,
//                                            materializer: Materializer
//                                          ) extends Logging {
//
//    /** @param resourceUrl the url pointing to gzipped resource
//      * @return an uncompressed InputStream, which will close when it reaches the end
//      */
//    def openGzippedResource(resourceUrl: String): Future[InputStream] = {
//      logger.debug(s"downloading $resourceUrl")
//      ws
//        .url(resourceUrl)
//        .withMethod("GET")
//        .withRequestTimeout(Duration.Inf)
//        .stream
//        .map(
//          _
//            .bodyAsSource
//            .async
//            .via(Compression.gunzip())
//            .runWith(StreamConverters.asInputStream(readTimeout = 20.seconds))
//        )
//    }
//  }

object JenkinsConnector {

//  sealed trait SeverityType
//
//  object SeverityType {
//    case object Ok extends SeverityType
//    case object Warning extends SeverityType
//    case object Critical extends SeverityType
//  }

//  case class Threshold(
//                        count: Long,
//                        severity: String
//                      )
//
//  case class HttpThreshold(
//                            count: Long,
//                            httpStatus: String,
//                            severity: SeverityType
//                          )
//
//  case class LogMessageThresholds(
//                                   count: Int,
//                                   lessThanMode: Boolean,
//                                   message: String
//                                 )
//
//  case class PercentageSplitThreshold(
//                                       absoluteThreshold: Int,
//                                       crossOver: Int,
//                                       errorFilter: String,
//                                       excludesSpikes: Int,
//                                       hysteresis: Double,
//                                       percentageThreshold: Double,
//                                       severity: String
//                                     )



}
