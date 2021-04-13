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

package uk.gov.hmrc.serviceconfigs.service

import akka.stream.Materializer
import play.api.Logging
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.serviceconfigs.connector.JenkinsConnector
import uk.gov.hmrc.serviceconfigs.model.{AlertEnvironmentHandler, LastJobNumber}
import uk.gov.hmrc.serviceconfigs.persistence.{AlertEnvironmentHandlerRepository, AlertJobNumberRepository}
import uk.gov.hmrc.serviceconfigs.service.AlertConfigSchedulerService.{processSensuConfig, processZip}

import java.io.{FileInputStream, FilterInputStream, InputStream}
import java.util.zip.{ZipEntry, ZipInputStream}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class AlertConfigSchedulerService @Inject() (alertEnvironmentHandlerRepository: AlertEnvironmentHandlerRepository,
                                             alertJobNumberRepository: AlertJobNumberRepository,
                                             jenkinsConnector: JenkinsConnector)(implicit val ec : ExecutionContext) {

  def updateConfigs(): Future[Unit] = {

    (for {
      jenkinsJobNumber  <- jenkinsConnector.getLatestJob().map(x => x.getOrElse(0))
      lastJobNumber     <- alertJobNumberRepository.findOne().map(x => x.getOrElse(LastJobNumber(0)).jobNumber)
      maybeJobNumber    =  if (jenkinsJobNumber > lastJobNumber) Option(jenkinsJobNumber) else None
    } yield maybeJobNumber).flatMap {
      case Some(jobNumber) =>
        for {
          zip           <- jenkinsConnector.getSensuZip()
          sensuConfig   = processZip(zip)
          alertHandlers = processSensuConfig(sensuConfig)
          _             <- alertEnvironmentHandlerRepository.insert(alertHandlers)
          ok            <- alertJobNumberRepository.update(jobNumber)
        } yield ok

      case None => Future.successful(())
    }

  }


    /*
      lastJobNumber [None || INT] :: alertJobNumberRepository -> get last Job Number
      jenkins -> get the latest Job Number

      maybe (lastJobNumber is empty) OR ( latest Job Number > last Job Number )
      DO
              configStream [MemStream] :: jenkins -> get config stream (by latestJobNumber)

              iterate json config files
              DO
                getFile configFile -> AlertEnvironmentHandler()
                with defaults

              iterate json handler files
                DO
                  iterate environment files
                    DO
                      getFile environmentFile ->

                      if (AlertConfig.handlers.contains(Handler.name))
                        DO
                        if Handler.command is enabled -> AlertEnvironmentHandler(enabled)
                        DO
                          UpdateInMemoryVariables
                          Insert to Mongo

             DO
              Update Job Number with latest
              ...
              Configs

      NOT
        -- end .
     */

}

case class AlertConfig(
                        app: String,
                        handlers: Seq[String]
                      )

object AlertConfig {
  implicit val formats: OFormat[AlertConfig] =
    Json.format[AlertConfig]
}

case class Handler(
                    command: String
                  )

object Handler {
  implicit val formats: OFormat[Handler] =
    Json.format[Handler]
}

case class SensuConfig(
                      alertConfigs: Seq[AlertConfig] = Seq.empty,
                      productionHandler: Map[String, Handler] = Map.empty
                      )

object AlertConfigSchedulerService {

  class NonClosableInputStream(inputStream: ZipInputStream) extends FilterInputStream(inputStream) {
    override def close(): Unit = {
      inputStream.closeEntry()
    }
  }

  def processZip(inputStream: InputStream): SensuConfig = {

    implicit val alertsReads = AlertConfig.formats
    implicit val handlerReads = Handler.formats

    val zip = new ZipInputStream(inputStream)

    Iterator.continually(zip.getNextEntry)
      .takeWhile(z => z != null)
      .foldLeft(SensuConfig())((config, entry) => {
        entry.getName match {
          case p if p.startsWith("output/configs/") && p.endsWith(".json")   => {
              val json = Json.parse(new NonClosableInputStream(zip))
              val newConfig = config.copy(alertConfigs = config.alertConfigs :+ json.as[AlertConfig])
              newConfig
          }
          case p if p.startsWith("output/handlers/aws_production") && p.endsWith(".json")  => {
            config.copy(productionHandler = (Json.parse(new NonClosableInputStream(zip)) \ "handlers").as[Map[String, Handler]])
          }
          case _ => config
        }
    })
  }

  def processSensuConfig(sensuConfig: SensuConfig): Seq[AlertEnvironmentHandler] = {
    sensuConfig.alertConfigs.map(alertConfig => {
      AlertEnvironmentHandler(serviceName = trimServiceName(alertConfig.app),
        production = alertConfig
          .handlers
          .exists(h => hasProductionHandler(sensuConfig.productionHandler, h)))
    })

  }

  def hasProductionHandler(productionHandlers: Map[String, Handler], handler: String): Boolean = {
    productionHandlers.get(handler).exists(p => !p.command.contains("noop.rb"))
  }

  def trimServiceName(service: String): String = {
    service.split('.').head
  }


//  val fis = new FileInputStream("/Users/samhmrcdigital/Downloads/sensuConfig.zip")
//  println(SensuConfig)
//  println(processSensuConfig(processZip(fis)))
}



//    var entry: ZipEntry = zip.getNextEntry()
//    var config = SensuConfig()
//
//    while( entry != null) {
//
//      entry.getName match {
//        case p if p.startsWith("output/configs/")   => {
//          implicit val reads = AlertConfig.formats
//          config = config.copy(alertConfigs = config.alertConfigs :+ Json.parse(new NonClosableInputStream(zip)).as[AlertConfig])
//        }
//        case p if p.startsWith("output/handlers/aws_production")  => {
//          implicit val reads = Handler.formats
//          config = config.copy(productionHandler = (Json.parse(new NonClosableInputStream(zip)) \ "handlers").as[Map[String, Handler]])
//        }
//        case _ => println("No Idea")
//      }
//
//      entry = zip.getNextEntry()
//    }
//
//    zip.close()
//    config
