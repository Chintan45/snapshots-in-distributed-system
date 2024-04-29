package utils

import org.slf4j.LoggerFactory

object MyLogger {
  class MyLogger (logger: org.slf4j.Logger) {
    def info(message: String): Unit = logger.info(message)
    def error(message: String): Unit = logger.error(s"\u001B[31m${message}\u001B[0m")
    def warn(message: String): Unit = logger.warn(s"\u001B[33m${message}\u001B[0m")
  }

  def getLogger(name: String): MyLogger = new MyLogger(LoggerFactory.getLogger(name))
}
