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

package uk.gov.hmrc.serviceconfigs.config

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.StringContextOps
import java.net.URL

@Singleton
class ArtefactReceivingConfig @Inject()(configuration: Configuration,
                                        serviceConfig: ServicesConfig) {

  private lazy val sqsQueueUrlPrefix  : URL = new URL(configuration.get[String]("artefact.receiver.aws.sqs.queue-prefix"))
  private lazy val sqsQueueSlugInfo   : String = configuration.get[String]("artefact.receiver.aws.sqs.queue-slug")

  lazy val sqsSlugQueue           = url"$sqsQueueUrlPrefix/$sqsQueueSlugInfo"
  lazy val sqsSlugDeadLetterQueue = url"$sqsQueueUrlPrefix/$sqsQueueSlugInfo-deadletter"
  lazy val isEnabled              = configuration.get[Boolean]("artefact.receiver.enabled")
}
