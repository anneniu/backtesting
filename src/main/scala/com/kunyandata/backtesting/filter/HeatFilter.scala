package com.kunyandata.backtesting.filter

import java.util.concurrent.{Callable, FutureTask}

import com.kunyandata.backtesting.io.RedisHandler
import com.kunyandata.backtesting.util.CommonUtil

import scala.collection.immutable
import scala.collection.mutable.ListBuffer

/**
  * Created by sijiansheng on 2016/10/11.
  */
class HeatFilter private(prefix: String, code: String, start: Int, end: Int, heatStandard: Int) extends Filter {

  def filter(): List[String] = {

    val jedis = RedisHandler.getInstance().getJedis
    val resultList = new ListBuffer[String]()

    for (i <- start to end) {

      val date = CommonUtil.getDateStr(i)
      val key = prefix + date
      val value = jedis.zscore(key, code)

      if (value != null && value > heatStandard) {
        resultList += (date + "," + value)
      }

    }

    resultList.toList
  }

}

object HeatFilter {

  def apply(prefix: String, code: String, start: Int, end: Int, heatStandard: Int): HeatFilter = {

    val filter = new HeatFilter(prefix, code, start, end, heatStandard)

    filter.futureTask = new FutureTask[List[String]](new Callable[List[String]] {
      override def call(): List[String] = filter.filter()
    })

    filter
  }
}