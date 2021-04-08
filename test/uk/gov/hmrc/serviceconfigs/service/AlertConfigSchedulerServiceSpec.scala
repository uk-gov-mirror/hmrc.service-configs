package uk.gov.hmrc.serviceconfigs.service

import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.serviceconfigs.model.AlertEnvironmentHandler

import java.io.FileInputStream

class AlertConfigSchedulerServiceSpec extends AnyWordSpec with Matchers
  with ScalaFutures with MockitoSugar {

  "AlertConfigSchedulerService.processZip" should {
    "iterate through files in zip with an input stream and produce a valid SensuConfig" in {

      val fis = new FileInputStream("./test/resources/outputOG.zip")
      val file = AlertConfigSchedulerService.processZip(fis)

      file shouldBe SensuConfig
    }
  }

  "AlertConfigSchedulerService.processSensuConfig" should {
    "produce an AlertEnvironmentHandler for a service that has alert config enabled" in {

      val sensuConfig = SensuConfig(Seq(AlertConfig("test.public.mdtp",
        Seq("TEST"))),
        Map("TEST" -> Handler("test/command")))

      AlertConfigSchedulerService.processSensuConfig(sensuConfig) shouldBe List(AlertEnvironmentHandler("test", true))

    }

    "produce an AlertEnvironmentHandler for a service that has Alert Config Disabled" in {

      val sensuConfig = SensuConfig(Seq(AlertConfig("test.public.mdtp",
        Seq("TEST"))),
        Map("TEST" -> Handler("test/noop.rb")))
      AlertConfigSchedulerService.processSensuConfig(sensuConfig) shouldBe List(AlertEnvironmentHandler("test", false))

    }

    "produce an AlertEnvironmentHandler when a service has No Matching Handler Found in Production" in {

      val sensuConfig = SensuConfig(Seq(AlertConfig("test.public.mdtp", Seq("TEST"))))
      AlertConfigSchedulerService.processSensuConfig(sensuConfig) shouldBe List(AlertEnvironmentHandler("test", false))

    }

    "produce an AlertEnvironmentHandler for a service that has No Handler Name Defined in Config" in {

      val sensuConfig = SensuConfig(Seq(AlertConfig("test.public.mdtp",
        Seq())),
        Map("TEST" -> Handler("test/command")))
      AlertConfigSchedulerService.processSensuConfig(sensuConfig) shouldBe List(AlertEnvironmentHandler("test", false))

    }

    "produce an empty list when there is No Existing Alert Config" in {

      val sensuConfig = SensuConfig(Seq(), Map("TEST" -> Handler("test/command")))
      AlertConfigSchedulerService.processSensuConfig(sensuConfig) shouldBe List.empty

    }


  }





}

// Test the zip
// Write test cases:
// Can we read the file, read the file find alert config(enabled (true), disabled(noop, false), no matching handler name)
//


//s"./src/test/resources/$filename"
//
//
//@RunWith(classOf[JUnitRunner])
//class ZipIntegrationTest extends Specification with TestData {
//
//  "ZipIntegrationTest" should {
//    "Compress a File, and them uncompress it" in {
//      val file1 = getResource("txt/lorem10.txt")
//      val file2 = getResource("img/github2.jpg")
//      val files = file1 :: file2 :: EmptyZip
//      println(files)
//      val zip = files.zipAs("./src/test/resources/stuff.zip")
//      val file = zip.unzipAs("./src/test/resources/stuff")
//      file.exists must_== true
//    }
//
//  }
//
//}