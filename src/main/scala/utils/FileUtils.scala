package utils

import utils.MyLogger.{MyLogger, getLogger}

import java.nio.file.{Files, Paths}
import scala.util.Try

object FileUtils {
  def writeToFile(filename: String, content: String): Unit = {
    val logger: MyLogger = getLogger(getClass.getName)

    val path = Paths.get(s"snapshots/$filename")
    Try {
      Files.write(path, content.getBytes)
    }.recover {
      case e: Exception =>
        logger.error(s"Failed to write snapshot to $filename: ${e.getMessage}")
    }
  }
}
