package com.kunyandata.backtesting

import java.util.NoSuchElementException
import java.util.concurrent.{ExecutorService, Executors}

import com.kunyandata.backtesting.config.{FilterType, Configuration}
import com.kunyandata.backtesting.filter._
import com.kunyandata.backtesting.io.{RedisHandler, KafkaProducerHandler, KafkaConsumerHandler}
import com.kunyandata.backtesting.logger.BKLogger
import com.kunyandata.backtesting.parser.Query
import com.kunyandata.backtesting.util.CommonUtil
import play.api.libs.json.Json

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by YangShuai
  * Created on 2016/8/23.
  */
object Scheduler {

  val threadPool: ExecutorService = Executors.newFixedThreadPool(20)

  def main(args: Array[String]): Unit = {

    if (args.length == 0) {
      BKLogger.error("未设置配置文件位置")
      return
    }

    val config = Configuration.getConfigurations(args(0))
    val redisMap = config._1
    val kafkaMap = config._2

    RedisHandler.init(redisMap.get("ip").get, redisMap.get("port").get.toInt, redisMap.get("auth").get, redisMap.get("db").get.toInt)

    val consumerHandler = KafkaConsumerHandler(kafkaMap.get("zookeeper").get, kafkaMap.get("groupId").get, kafkaMap.get("receiveTopic").get)
    val producerHandler = KafkaProducerHandler(kafkaMap.get("brokerList").get, kafkaMap.get("sendTopic").get)

    val streams = consumerHandler.getStreams

    for (stream <- streams) {

      val it = stream.iterator()

      while (it.hasNext()) {

        try {

          val beginTime = System.currentTimeMillis()
          val message = new String(it.next().message())
          println(message)
          val jsonValue = Json.parse(message)

          val uid = (jsonValue \ "uid").as[Long]
          val session = (jsonValue \ "session").as[Long]
          val pushTime = (jsonValue \ "push_time").as[Long]
          val condition = (jsonValue \ "condition").as[String]
          val startDate = (jsonValue \ "start_time").as[String]
          val endDate = (jsonValue \ "end_time").as[String]

          val queryMap = Query.parse(condition)
          System.out.println(queryMap)
          val result = filter(queryMap, startDate, endDate)

          var rightOption = condition

          result._2.split(",").foreach(x => {
            rightOption = rightOption.replaceFirst(x, "")
          })

          val finishTime = System.currentTimeMillis()
          val resultValue = Json.obj(
            "uid" -> uid,
            "session" -> session,
            "push_time" -> pushTime,
            "start_time" -> startDate,
            "end_time" -> endDate,
            "stocks" -> result._1,
            "wrong_condition" -> result._2,
            "right_condition" -> rightOption,
            "begin_time_stamp" -> beginTime,
            "finish_time_stamp" -> finishTime,
            "cost_time" -> (finishTime - beginTime)
          )

          producerHandler.sendMessage(Json.stringify(resultValue))

        } catch {
          case e: Exception =>
            e.printStackTrace()
        }

      }

    }

    RedisHandler.close()
    consumerHandler.shutdown()
    producerHandler.close()
  }

  def filter(queryMap: Map[Int, String], startDate: String, endDate: String): (mutable.ListBuffer[String], String) = {

    var wrongOption = queryMap.getOrElse(-1, "")
    val startOffset = CommonUtil.getOffset(startDate)
    val endOffset = CommonUtil.getOffset(endDate)

    var filters = ListBuffer[Filter]()
    var stockCodes = mutable.ListBuffer[String]()

    queryMap.foreach(pair => {

      var prefix = ""
      var filterType = ""

      val key = pair._1
      if (key > 0) {

        val values = pair._2.split(",")

        try {

          val infos = FilterType.apply(key).toString.split("\\|")
          prefix = infos(0)
          filterType = infos(1)

        } catch {

          case e: NoSuchElementException =>
          case e: IndexOutOfBoundsException =>
            e.printStackTrace()
            println(FilterType.apply(key).toString)

        }

        try {
          println("Value: " + FilterType.apply(key).toString)
        } catch {
          case e: NoSuchElementException =>
            println("Unknown enum id: " + key)
        }

        filterType match {
          case "all_days_value" =>
            filters += AllDayValueFilter(prefix, values(0).toDouble, values(1).toDouble, startOffset, endOffset)
          case "conti_value" =>
            filters += ContiValueFilter(prefix, values(0).toInt, values(1).toDouble, values(2).toDouble, startOffset, endOffset)
          case "conti_rank" =>
            filters += ContiRankFilter(prefix, values(0).toInt, values(1).toInt, startOffset, endOffset)
          case "single_value" =>
            filters += SingleValueFilter(prefix, values(0).toDouble, values(1).toDouble)
          case "sum_value" =>
            filters += SumValueFilter(prefix, values(0).toDouble, values(1).toDouble, startOffset, endOffset)
          case "direct" =>
            filters += SimpleUnionFilter(prefix, values(0), startOffset, endOffset)
          case "simple" =>
            filters += SimpleFilter(prefix, values(0))
          case "standard_deviation" =>
            if (startOffset == endOffset)
              filters += StandardDeviationFilter(prefix, values(0).toDouble, values(1).toInt, values(2).toInt, startOffset)
            else
              wrongOption += ",查询标准差的起始日期必须与结束日期相同"
          case _ =>
            println("unknown")
        }

      }

    })

    filters.foreach(filter =>
      threadPool.execute(filter.getFutureTask)
    )

    for (i <- filters.indices) {
      if (i == 0) {
        stockCodes ++= filters(i).getResult
      } else {
        stockCodes = stockCodes.intersect(filters(i).getResult)
      }
    }

    (stockCodes, wrongOption)
  }

}
