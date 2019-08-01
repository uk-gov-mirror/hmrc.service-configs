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

package uk.gov.hmrc.serviceconfigs.service

import alleycats.std.iterable._
import cats.instances.all._
import cats.syntax.all._
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.serviceconfigs.connector.{ConfigConnector, TeamsAndRepositoriesConnector}
import uk.gov.hmrc.serviceconfigs.model.DependencyConfig
import uk.gov.hmrc.serviceconfigs.parser.ConfigParser
import uk.gov.hmrc.serviceconfigs.persistence.{DependencyConfigRepository, SlugConfigurationInfoRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ConfigService @Inject()(configConnector: ConfigConnector,
                              teamsAndRepositoriesConnector: TeamsAndRepositoriesConnector,
                              slugConfigurationInfoRepository: SlugConfigurationInfoRepository,
                              dependencyConfigRepository: DependencyConfigRepository) {

  import ConfigService._

  private val localConfigSources = Seq(
    ConfigSource.ReferenceConf
    , ConfigSource.ApplicationConf
  )

  private val deployedConfigSources = Seq(
    ConfigSource.ReferenceConf
    , ConfigSource.ApplicationConf
    , ConfigSource.BaseConfig
    , ConfigSource.AppConfig
    , ConfigSource.AppConfigCommonFixed
    , ConfigSource.AppConfigCommonOverridable
  )

  val environments = Seq(
    Environment("local", localConfigSources)
    , Environment("development", deployedConfigSources)
    , Environment("qa", deployedConfigSources)
    , Environment("staging", deployedConfigSources)
    , Environment("integration", deployedConfigSources)
    , Environment("externaltest", deployedConfigSources)
    , Environment("production", deployedConfigSources)
  )


  private def getServiceType(configSourceEntries: Seq[ConfigSourceEntries]): Option[String] =
    configSourceEntries.find(_.source == ConfigSource.AppConfig.name).flatMap(_.entries.get("type"))

  private def configSourceEntries(environment: Environment, serviceName: String)(implicit hc: HeaderCarrier): Future[Seq[ConfigSourceEntries]] =
    environment.configSources.toIterable
      .foldLeftM[Future, Seq[ConfigSourceEntries]](Seq.empty) { case (seq, cs) =>
      cs
        .entries(configConnector, slugConfigurationInfoRepository, dependencyConfigRepository)(serviceName, environment.name, getServiceType(seq))
        .map(entries => seq :+ entries)
    }

  def configByEnvironment(serviceName: String)(implicit hc: HeaderCarrier): Future[ConfigByEnvironment] =
    environments.toIterable.foldLeftM[Future, ConfigByEnvironment](Map.empty) { (map, e) =>
      configSourceEntries(e, serviceName)
        .map(cse => map + (e.name -> cse))
    }

  // TODO consideration for deprecated naming? e.g. application.secret -> play.crypto.secret -> play.http.secret.key
  def configByKey(serviceName: String)(implicit hc: HeaderCarrier): Future[ConfigByKey] = {
    environments.toIterable.foldLeftM[Future, ConfigByKey](Map.empty) { case (map, e) =>
      configSourceEntries(e, serviceName).map { cses =>
        cses.foldLeft(map) { case (subMap, cse) =>
          subMap ++ cse.entries.map { case (key, value) =>
            val envMap = subMap.getOrElse(key, Map.empty)
            val values = envMap.getOrElse(e.name, Seq.empty)
            key -> (envMap + (e.name -> (values :+ ConfigSourceValue(cse.source, cse.precedence, value))))
          }
        }
      }
      // sort by keys
    }.map { map =>
      scala.collection.immutable.ListMap(map.toSeq.sortBy(_._1): _*)
    }
  }

  def serviceConfigKeysByValue(environmentName: String)(implicit hc: HeaderCarrier): Future[Map[String, scala.Seq[(KeyName, String)]]] =
    teamsAndRepositoriesConnector.allServices.flatMap(services => {
      val env = Environment("qa", deployedConfigSources)
      val entriesF = services.take(50).map(s =>
        configSourceEntries(env, s.name).map { cses => cses.map(cse => s.name -> cse) })

      val f = Future.sequence(entriesF).map(_.flatten)
      f.map(h =>
        h.foldLeft[Map[String, Seq[(KeyName, String)]]](Map.empty) { case (map, (serviceName, cse)) =>
          map ++ cse.entries.map { case (key, value) =>
            val valueSources = map.getOrElse(value, Seq())
            value -> (valueSources :+ (key -> serviceName))
          }
        })
    })
}

object ConfigService {
  type EnvironmentName = String
  type KeyName = String

  type ConfigByEnvironment = Map[EnvironmentName, Seq[ConfigSourceEntries]]
  type ConfigByKey = Map[KeyName, Map[EnvironmentName, Seq[ConfigSourceValue]]]
  type ServiceConfigKeysByValue = Map[String, Seq[ServiceConfigKey]]

  case class ConfigSourceEntries(source: String, precedence: Int, entries: Map[KeyName, String])

  case class ConfigSourceValue(source: String, precedence: Int, value: String)

  case class Environment(name: String, configSources: Seq[ConfigSource])

  case class ServiceConfigKey(keyName: KeyName, serviceName: String)

  sealed trait ConfigSource {
    def name: String

    def precedence: Int

    def entries(connector: ConfigConnector,
                slugConfigurationInfoRepository: SlugConfigurationInfoRepository,
                dependencyConfigRepository: DependencyConfigRepository)
               (serviceName: String, env: String, serviceType: Option[String])
               (implicit hc: HeaderCarrier): Future[ConfigSourceEntries]
  }

  object ConfigSource {

    case object ReferenceConf extends ConfigSource {
      val name = "referenceConf"
      val precedence = 9

      def entries(connector: ConfigConnector,
                  slugConfigurationInfoRepository: SlugConfigurationInfoRepository,
                  dependencyConfigRepository: DependencyConfigRepository)
                 (serviceName: String, env: String, serviceType: Option[String] = None)
                 (implicit hc: HeaderCarrier) =
        for {
          optSlugInfo <- slugConfigurationInfoRepository.getSlugInfo(serviceName)
          configs <- optSlugInfo match {
            case Some(slugInfo) =>
              Future.fold(slugInfo.dependencies
                .map(d => dependencyConfigRepository.getDependencyConfig(d.group, d.artifact, d.version)
                ))(Seq.empty[DependencyConfig])(_ ++ _)
            case None => Future(Seq.empty[DependencyConfig])
          }
          referenceConfig = ConfigParser.reduceConfigs(configs)
          entries = ConfigParser.flattenConfigToDotNotation(referenceConfig)
        } yield ConfigSourceEntries(name, precedence, entries)
    }

    case object ApplicationConf extends ConfigSource {
      val name = "applicationConf"
      val precedence = 10

      def entries(connector: ConfigConnector,
                  slugConfigurationInfoRepository: SlugConfigurationInfoRepository,
                  dependencyConfigRepository: DependencyConfigRepository)
                 (serviceName: String, env: String, serviceType: Option[String] = None)
                 (implicit hc: HeaderCarrier) =
        for {
          optSlugInfo <- slugConfigurationInfoRepository.getSlugInfo(serviceName)
          configs <- optSlugInfo match {
            case Some(slugInfo) =>
              Future.fold(slugInfo.dependencies
                .map(d => dependencyConfigRepository.getDependencyConfig(d.group, d.artifact, d.version)
                ))(Seq.empty[DependencyConfig])(_ ++ _)
            case None => Future(Seq.empty[DependencyConfig])
          }
          raw <- optSlugInfo match {
            case Some(slugInfo) => Future(slugInfo.applicationConfig)
            case None => connector.serviceApplicationConfigFile(serviceName) // if not slug info (e.g. java apps) get from github
          }
          applicationConf = ConfigParser.parseConfString(raw, ConfigParser.toIncludeCandidates(configs))
          entries = ConfigParser.flattenConfigToDotNotation(applicationConf)
        } yield ConfigSourceEntries(name, precedence, entries)
    }

    case object BaseConfig extends ConfigSource {
      val name = "baseConfig"
      val precedence = 20

      def entries(connector: ConfigConnector,
                  slugConfigurationInfoRepository: SlugConfigurationInfoRepository,
                  dependencyConfigRepository: DependencyConfigRepository)
                 (serviceName: String, env: String, serviceType: Option[String] = None)
                 (implicit hc: HeaderCarrier) =
        for {
          optSlugInfo <- slugConfigurationInfoRepository.getSlugInfo(serviceName)
          raw <- optSlugInfo match {
            case Some(slugInfo) => Future(slugInfo.slugConfig)
            case None => connector.serviceConfigConf("base", serviceName) // if no slug info (e.g. java apps) get from github
          }
          baseConf = ConfigParser.parseConfString(raw, logMissing = false) // ignoring includes, since we know this is applicationConf
          entries = ConfigParser.flattenConfigToDotNotation(baseConf)
        } yield ConfigSourceEntries(name, precedence, entries)
    }

    case object AppConfig extends ConfigSource {
      val name = "appConfigEnvironment"
      val precedence = 40

      def entries(connector: ConfigConnector,
                  slugConfigurationInfoRepository: SlugConfigurationInfoRepository,
                  dependencyConfigRepository: DependencyConfigRepository)
                 (serviceName: String, env: String, serviceType: Option[String] = None)
                 (implicit hc: HeaderCarrier) =
        for {
          raw <- connector.serviceConfigYaml(env, serviceName)
          entries = ConfigParser.parseYamlStringAsMap(raw).getOrElse(Map.empty)
            .map { case (k, v) => k.replace("hmrc_config.", "") -> v }
            .toMap
        } yield ConfigSourceEntries(name, precedence, entries)
    }

    case object AppConfigCommonFixed extends ConfigSource {
      val name = "appConfigCommonFixed"
      val precedence = 50

      def entries(connector: ConfigConnector,
                  slugConfigurationInfoRepository: SlugConfigurationInfoRepository,
                  dependencyConfigRepository: DependencyConfigRepository)
                 (serviceName: String, env: String, serviceType: Option[String] = None)
                 (implicit hc: HeaderCarrier) =
        for {
          raw <- serviceType.map(st => connector.serviceCommonConfigYaml(env, st)).getOrElse(Future.successful(""))
          entries = ConfigParser.parseYamlStringAsMap(raw).getOrElse(Map.empty)
            .filterKeys(_.startsWith("hmrc_config.fixed"))
            .map { case (k, v) => k.replace("hmrc_config.fixed.", "") -> v }
            .toMap
        } yield ConfigSourceEntries(name, precedence, entries)
    }

    case object AppConfigCommonOverridable extends ConfigSource {
      val name = "appConfigCommonOverridable"
      val precedence = 30

      def entries(connector: ConfigConnector,
                  slugConfigurationInfoRepository: SlugConfigurationInfoRepository,
                  dependencyConfigRepository: DependencyConfigRepository)
                 (serviceName: String, env: String, serviceType: Option[String] = None)
                 (implicit hc: HeaderCarrier) =
        for {
          raw <- serviceType.map(st => connector.serviceCommonConfigYaml(env, st)).getOrElse(Future.successful(""))
          entries = ConfigParser.parseYamlStringAsMap(raw).getOrElse(Map.empty)
            .filterKeys(_.startsWith("hmrc_config.overridable"))
            .map { case (k, v) => k.replace("hmrc_config.overridable.", "") -> v }
            .toMap
        } yield ConfigSourceEntries(name, precedence, entries)
    }

  }

}
