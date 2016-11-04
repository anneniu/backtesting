package com.kunyandata.backtesting.logger

import org.apache.log4j.{BasicConfigurator, Logger, PropertyConfigurator}

/**
  * Created by yangshuai on 2016/8/23.
  */
class RootLogger {

  var logger = Logger.getLogger("BACK_TESTING_ROOT")
  BasicConfigurator.configure()
  PropertyConfigurator.configure("/home/backtesting/conf/log4j.properties")

  var switch = true

  def exception(e: Exception) = {

    if (switch) {
      logger.error(e.getLocalizedMessage)
      logger.error(e)
      logger.error(e.getStackTrace)
    }

  }

  def error(msg: String): Unit = {
    if (switch)
      logger.error(msg)
  }

  def warn(msg: String): Unit = {
    if (switch)
      logger.warn(msg)
  }

  def info(msg: String): Unit = {
    if (switch)
      logger.info(msg)
  }

  def debug(msg: String): Unit = {
    if (switch)
      logger.debug(msg)
  }

}
