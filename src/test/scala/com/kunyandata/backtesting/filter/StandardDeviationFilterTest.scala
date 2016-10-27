package com.kunyandata.backtesting.filter

import com.kunyandata.backtesting.config.Configuration
import com.kunyandata.backtesting.io.RedisHandler
import com.kunyandata.backtesting.util.CommonUtil
import org.scalatest.{Matchers, FlatSpec}
import redis.clients.jedis.Jedis

import scala.collection.mutable.ArrayBuffer

/**
  * Created by sijiansheng on 2016/9/22.
  */
class StandardDeviationFilterTest extends FlatSpec with Matchers {

  it should "return a result that is equal the redis data" in {

    val path = ""

    val config = Configuration.getConfigurations(path)
    val redisMap = config._1
    RedisHandler.init(redisMap.get("ip").get, redisMap.get("port").get.toInt, redisMap.get("auth").get, redisMap.get("db").get.toInt)
    val jedis = RedisHandler.getInstance().getJedis

    //        val prefix = "count_heat_"
    val prefix = "industry_heat_"
    var meanPrefix = ""
    var stdPrefix = ""

    if (prefix.contains("industry")) {
      meanPrefix = "industry_"
      stdPrefix = "industry_"
    }

    val offset = -10
    val multiple = 3
    val cirterions = List(7, 10, 14, 15)

    for (cirterion <- cirterions) {

      val meanCriterion = cirterion
      val stdCriterion = cirterion
      val result = StandardDeviationFilter(prefix, multiple, meanCriterion, stdCriterion, offset).filter()

      for (code <- result) {

        println(code)
        val date = CommonUtil.getDateStr(offset)
        val score = jedis.zscore(prefix + date, code)
        val redisStd = jedis.zscore(stdPrefix + "heat_std_" + meanCriterion + "_" + date, code)
        val redisMean = jedis.zscore(meanPrefix + "heat_mean_" + meanCriterion + "_" + date, code)
        val newMeanAndStd = getMeanAndStd(meanCriterion, stdCriterion, jedis, offset, prefix, code)

        val newMean = newMeanAndStd._1
        val newStd = newMeanAndStd._2

        if (newMean.isEmpty || newStd.isEmpty) {
          System.exit(0)
        }

        println(redisMean)
        println(newMean.get)
        f"$redisMean%.6f" should be(f"${newMean.get}%.6f")
        f"$redisStd%.6f" should be(f"${newStd.get}%.6f")

        val lastResult = redisMean + multiple * redisStd
        val compareResult = score - lastResult

        compareResult.toInt should be >= 0
      }

    }

  }

  def getMean(redis: Jedis, prefix: String, offset: Int, value: String, scores: ArrayBuffer[Double]): Double = {

    var sum = 0d

    for (score <- scores) {
      sum += score
    }

    sum / scores.size
  }

  def getMean(criterion: Int, redis: Jedis, prefix: String, offset: Int, value: String): Double = {

    val scores = getScores(criterion, redis, offset, prefix, value)
    getMean(redis, prefix, offset, value, scores)
  }

  def getScores(n: Int, redis: Jedis, offset: Int, prefix: String, value: String): ArrayBuffer[Double] = {

    val scores = ArrayBuffer[Double]()

    for (i <- (offset - n).until(offset)) {

      val date = CommonUtil.getDateStr(i)
      val tempScore = redis.zscore(prefix + date, value)

      if (tempScore != null) {
        scores += tempScore
      }

    }

    scores
  }

  def getStd(mean: Double, redis: Jedis, offset: Int, prefix: String, value: String, scores: ArrayBuffer[Double]): Option[Double] = {

    if (scores.size == 1)
      return None

    val std = math.sqrt(scores.map(score => math.pow(score - mean, 2)).sum / (scores.size - 1))
    Option(std)
  }

  def getStd(criterion: Int, redis: Jedis, offset: Int, prefix: String, value: String): Option[Double] = {

    val scores = getScores(criterion, redis, offset, prefix, value)
    val mean = getMean(redis, prefix, offset, value, scores)

    getStd(mean, redis, offset, prefix, value, scores)
  }

  def getMeanAndStd(meanCriterion: Int, stdCriterion: Int, redis: Jedis, offset: Int, prefix: String, value: String): (Option[Double], Option[Double]) = {

    if (meanCriterion == stdCriterion) {
      val scores = getScores(meanCriterion, redis, offset, prefix, value)

      if (scores.isEmpty) {
        return (None, None)
      }

      val mean = getMean(redis, prefix, offset, value, scores)

      (Option(mean), getStd(mean, redis, offset, prefix, value, scores))
    } else {
      (Option(getMean(meanCriterion, redis, prefix, offset, value)), getStd(stdCriterion, redis, offset, prefix, value))
    }
  }


}

