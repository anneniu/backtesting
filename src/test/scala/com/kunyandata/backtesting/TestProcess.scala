package com.kunyandata.backtesting

import com.kunyandata.backtesting.config.Configuration
import com.kunyandata.backtesting.io.{KafkaProducerHandler, KafkaConsumerHandler, RedisHandler}
import com.kunyandata.backtesting.parser.Query

import scala.collection.mutable

/**
  * Created by niujiaojiao on 2016/9/5.
  */
object TestProcess extends App {

  val config = Configuration.getConfigurations(args(0))
  val redisMap = config._1
  val kafkaMap = config._2
  RedisHandler.init(redisMap.get("ip").get, redisMap.get("port").get.toInt, redisMap.get("auth").get, redisMap.get("db").get.toInt)
  val consumerHandler = KafkaConsumerHandler(kafkaMap.get("zookeeper").get, kafkaMap.get("groupId").get, kafkaMap.get("receiveTopic").get)
  val producerHandler = KafkaProducerHandler(kafkaMap.get("brokerList").get, kafkaMap.get("sendTopic").get)

  val conditionsGroupOne = List(
    "1:总股本大于5000万小于5500万",
    "1:流通股本大于1000万小于1200万",
    "1:总市值大于50000亿小于100000亿",
    "1:流通市值大于20000亿小于30000亿",
    "1:股东户数大于5万小于20万",
    "1:十大股东持股比例大于0.02%小于0.04%",
    "1:户均持股数大于0万小于500万"
  )

  val resultsGroupOne = List(
    "300501",
    "300508",
    "300372",
    "300372",
    "002803",
    "600868",
    "603726")

  for (index <- conditionsGroupOne.indices) {

    val queryMap = Query.parse(conditionsGroupOne(index))
    val result = Scheduler.filter(queryMap, "2015-06-12", "2015-06-12")
    val stocks = result._1
    assert(stocks.size == 1)
    assert(stocks.head == resultsGroupOne(index))

  }

  val conditionGroupTwo = List(
    "2:涨跌幅大于0.08%小于0.09%",
    "2:跌幅大于0.06%小于0.07%",
    "2:振幅大于0.08%小于0.09%",
    "2:成交量大于60万小于80万",
    "2:股价大于11元小于12元",
    "2:收益率大于-0.07%小于-0.06%"
  )

  val resultsGroupTwo = List(
    "002347",
    "300441",
    "002044",
    "300466",
    "600339",
    "300441")

  for (index <- conditionGroupTwo.indices) {

    val queryMap = Query.parse(conditionGroupTwo(index))
    val result = Scheduler.filter(queryMap, "2015-06-12", "2015-06-12")
    val stocks = result._1
    assert(stocks.size == 1)
    assert(stocks.head == resultsGroupTwo(index))

  }

  var queryMap = Query.parse("1:流通比例大于0.03%小于0.04%")
  var result = Scheduler.filter(queryMap, "2016-08-05", "2016-08-05")
  var stocks = result._1
  assert(stocks.size == 2)
  assert(stocks.toList == List("002558", "601939"))

  queryMap = Query.parse("2:涨幅大于0.055%小于0.06%")
  result = Scheduler.filter(queryMap, "2015-06-12", "2015-06-12")
  stocks = result._1
  assert(stocks.size == 1)
  assert(stocks.head == "000876")

  queryMap = Query.parse("2:换手率大于0.02%小于0.14%")
  result = Scheduler.filter(queryMap, "2015-06-12", "2015-06-12")
  stocks = result._1
  assert(stocks.size == 2)
  assert(stocks.toList == List("300466", "300110"))


  queryMap = Query.parse("5:石墨烯")
  result = Scheduler.filter(queryMap, "2016-08-25", "2016-08-25")
  stocks = result._1
  assert(stocks.size == 1)
  assert(stocks.toList == List("601519"))

  queryMap = Query.parse("1:机构持股数大于520000万小于530000万")
  result = Scheduler.filter(queryMap, "2016-08-25", "2016-08-25")
  stocks = result._1
  assert(stocks.size == 1)
  assert(stocks.toList == List("002792"))

  queryMap = Query.parse("4:高管增股")
  println(queryMap)
  result = Scheduler.filter(queryMap, "2016-08-25", "2016-08-25")
  stocks = result._1
  assert(stocks.size == 1165)
  assert(stocks.head == "000002")

  queryMap = Query.parse("4:高管减股")
  result = Scheduler.filter(queryMap, "2016-08-25", "2016-08-25")
  stocks = result._1
  assert(stocks.size == 1696)
  assert(stocks.head == "000001")

  queryMap = Query.parse("4:新闻访问热度每天大于77次小于80次")
  result = Scheduler.filter(queryMap, "2016-08-05", "2016-08-05")
  stocks = result._1
  assert(stocks.size == 1)
  assert(stocks.head == "600327")

  queryMap = Query.parse("4:新闻访问热度每周大于112次小于114次")
  result = Scheduler.filter(queryMap, "2016-08-01", "2016-08-01")
  stocks = result._1
  assert(stocks.size == 2)
  assert(stocks.toList == List("601601", "600185"))

  queryMap = Query.parse("4:新闻访问热度每月大于292次小于293次")
  result = Scheduler.filter(queryMap, "2016-08-01", "2016-08-01")
  stocks = result._1
  assert(stocks.size == 3)
  assert(stocks.toList == List("600499", "300452", "002587"))

  queryMap = Query.parse("4:新闻转载热度每天大于38次小40次")
  result = Scheduler.filter(queryMap, "2016-08-01", "2016-08-01")
  stocks = result._1
  assert(stocks.size == 1)
  assert(stocks.head == "600016")

  queryMap = Query.parse("4:新闻转载热度每周大于350次小于500次")
  result = Scheduler.filter(queryMap, "2016-08-01", "2016-08-01")
  stocks = result._1
  assert(stocks.size == 1)
  assert(stocks.head == "300033")

  queryMap = Query.parse("4:新闻转载热度每月大于2000次小于4000次")
  result = Scheduler.filter(queryMap, "2016-08-01", "2016-08-01")
  stocks = result._1
  assert(stocks.size == 1)
  assert(stocks.head == "300033")

  queryMap = Query.parse("4:新闻趋势连续5天上涨")
  result = Scheduler.filter(queryMap, "2016-08-01", "2016-08-31")
  stocks = result._1
  assert(stocks.size == 25)
  assert(stocks.toList == List("002237", "600019", "000550", "300027", "601069", "600068", "000671", "603766", "601668", "000898", "600288", "002264", "002055", "002145", "300024", "600782", "000959", "600663",
    "601857", "601318", "002244", "601390", "300340", "002284", "300298"))

  queryMap = Query.parse("4:新闻趋势连续5天以上上涨")
  result = Scheduler.filter(queryMap, "2016-07-05", "2016-07-10")
  stocks = result._1
  assert(stocks.size == 2)
  assert(stocks.toList == List("600048", "601099"))

  queryMap = Query.parse("4:新闻情感连续7天都是负面情绪")
  result = Scheduler.filter(queryMap, "2016-07-05", "2016-07-15")
  stocks = result._1
  assert(stocks.size == 4)
  var compares = List("300268", "600919", "000800", "300372")
  assert(stocks.toList == compares)

  queryMap = Query.parse("4:新闻情感连续7天以上都是负面情绪")
  result = Scheduler.filter(queryMap, "2016-07-05", "2016-07-15")
  stocks = result._1
  assert(stocks.size == 4)
  compares = List("300268", "600919", "000800", "300372")
  assert(stocks.toList == compares)

  queryMap = Query.parse("4:连续5天被1个大V看好")
  result = Scheduler.filter(queryMap, "2016-07-01", "2016-08-30")
  stocks = result._1
  assert(stocks.size == 1)
  assert(stocks.toList == List("300247"))

  queryMap = Query.parse("4:连续3天被1个大V看空")
  result = Scheduler.filter(queryMap, "2016-07-01", "2016-08-30")
  stocks = result._1
  assert(stocks.size == 10)
  compares = List("603818", "600676", "601336", "000728", "600048", "600348", "002647", "002539", "603868", "002303")
  assert(stocks.toList == compares)

  queryMap = Query.parse("4:连续4天以上被1个大V看好")
  result = Scheduler.filter(queryMap, "2016-07-01", "2016-08-30")
  stocks = result._1
  assert(stocks.size == 1)
  assert(stocks.toList == List("300247"))

  queryMap = Query.parse("4:连续3天以上被1个大V看空")
  result = Scheduler.filter(queryMap, "2016-07-01", "2016-08-30")
  stocks = result._1
  assert(stocks.size == 10)
  assert(stocks.toList == List("603818", "600676", "601336", "000728", "600048", "600348", "002647", "002539", "603868", "002303"))

  queryMap = Query.parse("4:连续4天被1个大V以上看好")
  result = Scheduler.filter(queryMap, "2016-07-01", "2016-08-30")
  stocks = result._1
  assert(stocks.size == 24)
  assert(stocks.toList.head == "600141")

  queryMap = Query.parse("4:连续4天被1个大V以上看空")
  result = Scheduler.filter(queryMap, "2016-07-01", "2016-08-30")
  stocks = result._1
  assert(stocks.size == 1)
  assert(stocks.toList == List("600149"))

  queryMap = Query.parse("4:连续2~3天被1个大V看好")
  result = Scheduler.filter(queryMap, "2016-07-01", "2016-08-30")
  stocks = result._1
  assert(stocks.size == 89)
  assert(stocks.toList.head == "601398")

  queryMap = Query.parse("4:连续2~3天被1个大V看空")
  result = Scheduler.filter(queryMap, "2016-07-01", "2016-08-30")
  stocks = result._1
  assert(stocks.size == 24)
  assert(stocks.toList.head == "600141")

  queryMap = Query.parse("4:连续2天被1~2个大V看好")
  result = Scheduler.filter(queryMap, "2016-07-01", "2016-08-30")
  stocks = result._1
  assert(stocks.size == 103)
  assert(stocks.toList.head == "601398")

  queryMap = Query.parse("4:连续2天被1~2个大V看空")
  result = Scheduler.filter(queryMap, "2016-07-01", "2016-08-30")
  stocks = result._1
  assert(stocks.size == 26)
  assert(stocks.toList.head == "600141")

  queryMap = Query.parse("4:查看热度连续2天上涨超过100000")
  result = Scheduler.filter(queryMap, "2016-07-01", "2016-08-30")
  stocks = result._1
  compares = List("000002")
  assert(stocks.toList == compares)

  queryMap = Query.parse("4:查看热度连续2天以上上涨超过100000")
  result = Scheduler.filter(queryMap, "2016-07-01", "2016-08-30")
  stocks = result._1
  compares = List("000002")
  assert(stocks.toList == compares)

  queryMap = Query.parse("4:查看热度连续4天以上出现在top5")
  result = Scheduler.filter(queryMap, "2016-07-01", "2016-08-30")
  stocks = result._1
  compares = List("000002", "300372")
  assert(stocks.toList == compares)

  queryMap = Query.parse("4:查看热度连续4天超过100000")
  result = Scheduler.filter(queryMap, "2016-07-01", "2016-08-30")
  stocks = result._1
  compares = List("000002")
  assert(stocks.toList == compares)

  queryMap = Query.parse("4:查看热度连续5天以上超过100000")
  result = Scheduler.filter(queryMap, "2016-07-01", "2016-08-30")
  stocks = result._1
  compares = List("000002")
  assert(stocks.toList == compares)

  queryMap = Query.parse("4:查看热度连续4天出现在top5")
  result = Scheduler.filter(queryMap, "2016-07-01", "2016-08-30")
  stocks = result._1
  compares = List("000002", "300372")
  assert(stocks.toList == compares)

  queryMap = Query.parse("4:盈利预增1次")
  result = Scheduler.filter(queryMap, "2016-08-01", "2016-08-26")
  stocks = result._1
  assert(stocks.size == 12)
  assert(stocks.toList.head == "603011")

  queryMap = Query.parse("4:诉讼仲裁1次")
  result = Scheduler.filter(queryMap, "2016-07-01", "2016-08-30")
  stocks = result._1
  assert(stocks.size == 93)
  assert(stocks.toList.head == "002204")

  queryMap = Query.parse("4:违规处罚1次")
  result = Scheduler.filter(queryMap, "2016-07-01", "2016-08-30")
  stocks = result._1
  assert(stocks.size == 75)
  assert(stocks.toList.head == "002324")

  queryMap = Query.parse("4:盈利预增1次以上")
  result = Scheduler.filter(queryMap, "2016-07-01", "2016-08-30")
  stocks = result._1
  assert(stocks.size == 49)
  assert(stocks.toList.head == "600765")

  queryMap = Query.parse("4:诉讼仲裁1次以上")
  result = Scheduler.filter(queryMap, "2016-07-01", "2016-08-30")
  stocks = result._1
  assert(stocks.size == 117)
  assert(stocks.toList.head == "000048")

  queryMap = Query.parse("4:违规处罚1次以上")
  result = Scheduler.filter(queryMap, "2016-07-01", "2016-08-30")
  stocks = result._1
  assert(stocks.size == 88)
  assert(stocks.toList.head == "002324")

  consumerHandler.shutdown()
  producerHandler.close()

}
