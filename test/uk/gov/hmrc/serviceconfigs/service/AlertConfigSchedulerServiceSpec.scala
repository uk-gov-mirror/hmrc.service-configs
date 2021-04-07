package uk.gov.hmrc.serviceconfigs.service

import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.FileInputStream

class AlertConfigSchedulerServiceSpec extends AnyWordSpec with Matchers
  with ScalaFutures with MockitoSugar {

  "AlertConfigSchedulerService" should {
    "process zip as an input stream and produce a valid SensuConfig" in {

      val fis = new FileInputStream("./test/resources/happy-output.zip")
      val file = AlertConfigSchedulerService.processZip(fis)
      file shouldBe SensuConfig
    }
  }

}


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