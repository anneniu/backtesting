package com.kunyandata.backtesting.filter

import com.kunyandata.backtesting.config.Configuration
import com.kunyandata.backtesting.io.RedisHandler
import com.kunyandata.backtesting.util.CommonUtil
import org.scalatest.{FlatSpec, Matchers}
import redis.clients.jedis.Jedis

import scala.collection.mutable.ArrayBuffer

/**
  * Created by sijiansheng on 2016/9/22.
  */
class HeatFilterTest extends FlatSpec with Matchers {

  it should "return a result that is greater the heat standard" in {

    val path = ""
    val heatStandard = 1000
    val config = Configuration.getConfigurations(path)
    val redisMap = config._1
    RedisHandler.init(redisMap.get("ip").get, redisMap.get("port").get.toInt, redisMap.get("auth").get, redisMap.get("db").get.toInt)
    val jedis = RedisHandler.getInstance().getJedis
    val result = HeatFilter("count_heat_", "002018", -10, -3, heatStandard).filter()

    result.foreach(result => {
      println(result)
      val dateAndHeat = result.split(",")
      val date = dateAndHeat(0)
      println(dateAndHeat(1))
      val heat = dateAndHeat(1).toInt
      heat should be > heatStandard
    })

  }

}

