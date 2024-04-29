package utils

import utils.FileUtils

import java.nio.file.{Files, Path, Paths}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach

import scala.util.Using


class FileUtilsTest extends AnyFlatSpec with Matchers with BeforeAndAfterEach {
  val testFilename = "testfile.txt"
  val testContent = "Hello, world!"
  val testPath = Paths.get(s"snapshots/$testFilename")

  override def afterEach(): Unit = {
    Files.deleteIfExists(testPath)
  }

  "writeToFile" should "successfully write content to a file" in {
    FileUtils.writeToFile(testFilename, testContent)

    // Verify the file exists and the content is correct
    assert(Files.exists(testPath))
    val fileContent = Using(Files.newBufferedReader(testPath)) { reader =>
      reader.readLine()
    }.getOrElse("")

    fileContent should be(testContent)
  }

  val testFilename2 = "testfile.txt"
  val testContent2: String = """{"name": "Test User", "age": 30}"""
  val testPath2: Path = Paths.get(s"snapshots/$testFilename2")


  "writeToFile" should "successfully write json to a file" in {
    FileUtils.writeToFile(testFilename2, testContent2)

    // Verify the file exists and the content is correct
    assert(Files.exists(testPath2))
    val fileContent = Using(Files.newBufferedReader(testPath2)) { reader =>
      reader.readLine()
    }.getOrElse("")

    fileContent should be(testContent2)
  }

}
