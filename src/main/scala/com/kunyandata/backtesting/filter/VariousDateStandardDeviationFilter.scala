package com.kunyandata.backtesting.filter

import java.util.concurrent.{Callable, FutureTask}

import com.kunyandata.backtesting.io.RedisHandler
import com.kunyandata.backtesting.util.CommonUtil

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
class VariousDateStandardDeviationFilter private(prefix: String, ratio: Double, meanValue: Int, standardDeviation: Int, startDay: Int, endDay: Int) extends Filter {

  override def filter(): List[String] = {

    val jedis = RedisHandler.getInstance().getJedis
    val resultMap = mutable.HashMap[String, String]()

    for (day <- startDay to endDay) {

      val result = StandardDeviationFilterUtil.getStock(prefix, ratio, meanValue, standardDeviation, jedis, day)

      result.foreach(stock => {

        var date = CommonUtil.getDateStr(day)

        if (resultMap.contains(stock)) {
          date = resultMap(stock) + "," + date
        }

        resultMap.put(stock, date)

      })

    }

    val reusltList = resultMap.map(stockAndDate => stockAndDate._1 + "->" + stockAndDate._2).toList
    jedis.close()

    reusltList
  }

}

object VariousDateStandardDeviationFilter {

  def apply(prefix: String, multiple: Double, meanCriterion: Int, stdCriterion: Int, startDay: Int, endDay: Int): VariousDateStandardDeviationFilter = {

    val filter = new VariousDateStandardDeviationFilter(prefix, multiple, meanCriterion, stdCriterion, startDay, endDay)

    filter.futureTask = new FutureTask[List[String]](new Callable[List[String]] {
      override def call(): List[String] = filter.filter()
    })

    filter
  }
}
