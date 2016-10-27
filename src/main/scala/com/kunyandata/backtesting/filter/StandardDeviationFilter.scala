package com.kunyandata.backtesting.filter

import java.text.SimpleDateFormat
import java.util.concurrent.{Callable, FutureTask}

import com.kunyandata.backtesting.io.RedisHandler
import com.kunyandata.backtesting.logger.BKLogger
import com.kunyandata.backtesting.util.CommonUtil
import redis.clients.jedis.exceptions.JedisDataException
import redis.clients.jedis.{Jedis, Tuple}

import scala.collection.mutable

/**
  * Created by sijiansheng on 2016/9/21.
  * 热度离均差过滤器和行业热度离均差过滤器
  * 日均查看热度离均差（MA30）大于 x 倍前30天日均热度标准差
  * prefix redis key前缀
  * ratio 比率 XX倍
  * meanValue 平均值计算标准，如30天平均值用30表示 5天平均值用5表示
  * standardDeviation 标准差计算标准，如30天标准差用30表示 5天标准差用5表示
  * day查询日期的偏移量
  */
class StandardDeviationFilter private(prefix: String, ratio: Double, meanValue: Int, standardDeviation: Int, day: Int) extends Filter {

  override def filter(): List[String] = {

    val jedis = RedisHandler.getInstance().getJedis
    val resultSet = StandardDeviationFilterUtil.getStock(prefix, ratio, meanValue, standardDeviation, jedis, day)
    jedis.close()

    resultSet.toList
  }

}

object StandardDeviationFilter {

  def apply(prefix: String, multiple: Double, meanCriterion: Int, stdCriterion: Int, day: Int): StandardDeviationFilter = {

    val filter = new StandardDeviationFilter(prefix, multiple, meanCriterion, stdCriterion, day: Int)

    filter.futureTask = new FutureTask[List[String]](new Callable[List[String]] {
      override def call(): List[String] = filter.filter()
    })

    filter
  }
}

object StandardDeviationFilterUtil {

  def getStock(prefix: String, ratio: Double, meanValue: Int, standardDeviation: Int, jedis: Jedis, day: Int): mutable.Set[String] = {

    var meanPrefix = ""
    var standardDeviationPrefix = ""

    if (prefix.contains("industry")) {
      meanPrefix = "industry_"
      standardDeviationPrefix = "industry_"
    }

    val date = CommonUtil.getDateStr(day)
    //获得的某天的所有的离均差
    val valuesAndScores = valueAndScoreToMap(jedis.zrangeByScoreWithScores(prefix + date, Double.MinValue, Double.MaxValue))

    val resultSet = compareHeat(ratio, valuesAndScores, jedis, meanPrefix, meanValue, standardDeviationPrefix, standardDeviation, date)

    resultSet
  }

  def valueAndScoreToMap(set: java.util.Set[Tuple]): mutable.Map[String, Double] = {

    val resultMap = mutable.Map[String, Double]()
    val iterator = set.iterator

    while (iterator.hasNext) {
      val valueAndScore = iterator.next()
      resultMap += (valueAndScore.getElement -> valueAndScore.getScore)
    }

    resultMap
  }

  /**
    * 把热度和标准差平均值的热度进行比较
    *
    * @param ratio                   比较标准
    * @param conditionHeat           获得的热度的集合
    * @param jedis
    * @param meanPrefix              平均值key名在redis中的前缀 如“industry_”
    * @param meanValue               比较的平均值标准 7天14天30天等
    * @param standardDeviationPrefix 标准差key名在redis中的前缀
    * @param standardDeviation       比较的标准差标准 7天14天30天嗯
    * @param date                    查询的标准差和平均值的日期
    * @return 符合比较条件的股票集合
    */
  def compareHeat(ratio: Double, conditionHeat: mutable.Map[String, Double], jedis: Jedis, meanPrefix: String, meanValue: Int, standardDeviationPrefix: String, standardDeviation: Int, date: String): mutable.Set[String] = {

    var resultSet = mutable.Set[String]()
    var meanValuesAndScoresMap = mutable.Map[String, Double]()
    var standardDeviationValuesAndScoresMap = mutable.Map[String, Double]()

    try {

      //获得的某天的所有的股票的平均值的map集合，key为股票代码，value为平均值
      val newMap = valueAndScoreToMap(jedis.zrangeByScoreWithScores(meanPrefix + "heat_mean_" + meanValue + "_" + date, Double.MinValue, Double.MaxValue))
      meanValuesAndScoresMap = valueAndScoreToMap(jedis.zrangeByScoreWithScores(meanPrefix + "heat_mean_" + meanValue + "_" + date, Double.MinValue, Double.MaxValue))
      //获得的某天的所有股票的标准差的map集合，key为股票代码，value为标准差
      standardDeviationValuesAndScoresMap = valueAndScoreToMap(jedis.zrangeByScoreWithScores(standardDeviationPrefix + "heat_std_" + standardDeviation + "_" + date, Double.MinValue, Double.MaxValue))
    } catch {

      case e: JedisDataException =>

        BKLogger.warn("查询的前天热度在redis中不存在，返回两天前的redis数据")
        val newdate = new SimpleDateFormat("yyyy-MM-dd").format(new SimpleDateFormat("yyyy-MM-dd").parse(date).getTime - 1000 * 60 * 60 * 24)
        meanValuesAndScoresMap = valueAndScoreToMap(jedis.zrangeByScoreWithScores(meanPrefix + "heat_mean_" + meanValue + "_" + newdate, Double.MinValue, Double.MaxValue))
        standardDeviationValuesAndScoresMap = valueAndScoreToMap(jedis.zrangeByScoreWithScores(standardDeviationPrefix + "heat_std_" + standardDeviation + "_" + newdate, Double.MinValue, Double.MaxValue))

    }

    conditionHeat.foreach(stockAndHeat => {

      val stock = stockAndHeat._1
      val score = stockAndHeat._2
      val meanScore = meanValuesAndScoresMap.getOrElse(stock, Double.MaxValue)
      val standardDeviationScore = standardDeviationValuesAndScoresMap.getOrElse(stock, Double.MaxValue)

      if (meanScore != Double.MaxValue && standardDeviationScore != Double.MaxValue) {

        if (score.toDouble > (ratio * standardDeviationScore + meanScore)) {
          resultSet += stock
        }

      }

    })

    resultSet
  }

}
