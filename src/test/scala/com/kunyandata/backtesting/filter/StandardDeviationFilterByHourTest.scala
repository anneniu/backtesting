package com.kunyandata.backtesting.filter

import com.kunyandata.backtesting.config.Configuration
import com.kunyandata.backtesting.io.RedisHandler
import com.kunyandata.backtesting.util.CommonUtil
import org.scalatest.{Matchers, FlatSpec}

/**
  * Created by sijiansheng on 2016/10/20.
  */
class StandardDeviationFilterByHourTest extends FlatSpec with Matchers {

  it should "return a result and println" in {

    val path = ""

    val config = Configuration.getConfigurations(path)
    val redisMap = config._1
    RedisHandler.init(redisMap.get("ip").get, redisMap.get("port").get.toInt, redisMap.get("auth").get, redisMap.get("db").get.toInt)

    val prefix = "count_heat_"
    //    val prefix = "industry_heat_"

    val cirterions = List(7, 10, 14, 15)
    val multiple = 1

    for (cirterion <- cirterions) {

      val meanCriterion = cirterion
      val stdCriterion = cirterion
      val result = StandardDeviationFilterByHour(prefix, multiple, meanCriterion, stdCriterion, 1476806400000L, 1476892800000L, 10, 10).filter()

      for (code <- result) {
        println(code)
      }

      println("#############################")
    }
  }
}