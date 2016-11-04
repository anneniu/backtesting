package com.kunyandata.backtesting.filter

import java.text.SimpleDateFormat
import java.util.concurrent.{Callable, FutureTask}

import com.kunyandata.backtesting.io.RedisHandler
import redis.clients.jedis.Jedis

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * redis中按小时求得的热度值之和和标准差平均值的热度比较
  *
  * @param prefix            redis中热度值key的前缀，主要区别是否是产业热度值即是否包含industry
  * @param ratio             比率 倍数即标准差的乘数
  * @param meanValue         平均值标准 7 14 30等即7天平均值 14天平均值 30天平均值
  * @param standardDeviation 标准差标准 7，14 30同上
  * @param startDay          起始日期 没用到
  * @param endDay            结束日期 没用到
  * @param startDateWithHour 查询的起始日期和小时
  * @param endDateWithHour   查询的结束日期和小时
  */
class StandardDeviationFilterByHour private(prefix: String, ratio: Double, meanValue: Int, standardDeviation: Int, startDateWithHour: Long, endDateWithHour: Long, startDay: Int, endDay: Int) extends Filter {

  override def filter(): List[String] = {

    val jedis = RedisHandler.getInstance().getJedis
    val redisKeys = StandardDeviationFilterByHour.getRedisKey(startDateWithHour, endDateWithHour)

    var meanPrefix = ""
    var standardDeviationPrefix = ""

    //区别产业热度和股票热度
    if (prefix.contains("industry")) {
      meanPrefix = "industry_"
      standardDeviationPrefix = "industry_"
    }

    //得到符合条件的股票热度之和，返回股票和热度的map集合
    val resultMap = StandardDeviationFilterByHour.getConditionalHeatMap(redisKeys, jedis)

    val date = new SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis() - 1000 * 60 * 60 * 24)

    val resultSet = StandardDeviationFilterUtil.compareHeat(ratio, resultMap, jedis, meanPrefix, meanValue, standardDeviationPrefix, standardDeviation, date)

    jedis.close()

    resultSet.toList
  }

}

object StandardDeviationFilterByHour {

  def apply(prefix: String, multiple: Double, meanCriterion: Int, stdCriterion: Int, startDateWithHour: Long, endDateWithHour: Long, startDay: Int, endDay: Int): StandardDeviationFilterByHour = {

    val filter = new StandardDeviationFilterByHour(prefix, multiple, meanCriterion, stdCriterion, startDateWithHour, endDateWithHour, startDay, endDay)

    filter.futureTask = new FutureTask[List[String]](new Callable[List[String]] {
      override def call(): List[String] = filter.filter()
    })

    filter
  }

  /**
    * 根据起止的日期小时得到中间所有的日期和时间间隔
    *
    * @param startTimestamp 开始时间戳
    * @param endTimestamp   结束时间 格式同上
    * @return 返回时间间隔内的所有小时 如：开始时间为2010101008  结束时间为2010101010  则返回的结果时List("2010101008","2010101009","2010101010")
    */
  def getAllHourByStartAndEnd(startTimestamp: Long, endTimestamp: Long): List[String] = {

    val result = ListBuffer[String]()

    for (time <- (startTimestamp / 1000 / 60 / 60) to (endTimestamp / 1000 / 60 / 60)) {
      result += getDateWithHourStringByTimestamp(time * 1000 * 60 * 60)
    }

    result.toList
  }

  def getTimestampByDateWithHour(dateWithHour: String): Long = new SimpleDateFormat("yyyyMMddHH").parse(dateWithHour).getTime

  def getDateWithHourStringByTimestamp(timestamp: Long): String = new SimpleDateFormat("yyyy-MM-dd-HH").format(timestamp)

  def getRedisKey(startTime: Long, endTime: Long): List[String] = getAllHourByStartAndEnd(startTime, endTime).map("count_heat_hour_" + _)

  /**
    * 从redis中得到股票和该股票在redis所有key的热度的和值
    *
    * @param redisKeys redis key
    * @param jedis     jedis连接
    * @return 股票和该股票在redis所有key的热度的和值
    */
  def getConditionalHeatMap(redisKeys: List[String], jedis: Jedis): mutable.Map[String, Double] = {

    val resultMap = mutable.Map[String, Double]()

    val redisData = redisKeys.map(redisKey => {
      StandardDeviationFilterUtil.valueAndScoreToMap(jedis.zrangeByScoreWithScores(redisKey, Double.MinValue, Double.MaxValue))
    })


    redisData.foreach(redisCell => {

      redisCell.foreach(

        stockAndHeat => {
          val stock = stockAndHeat._1
          val heat = stockAndHeat._2

          if (resultMap.contains(stock)) {
            val lastHeat = resultMap.getOrElse(stock, 0.0)
            resultMap.put(stock, lastHeat + heat)
          } else {
            resultMap.put(stock, heat)
          }

        }

      )

    })

    resultMap
  }

}
