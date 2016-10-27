package com.kunyandata.backtesting.filter

import java.util.concurrent.{Callable, FutureTask}

import com.kunyandata.backtesting.io.RedisHandler
import com.kunyandata.backtesting.util.CommonUtil

import scala.collection.mutable

/**
  * 对给定日期范围内值得总和的过滤
  * Created by YangShuai
  * Created on 2016/9/2.
  */
class SumValueFilter private(prefix: String, min: Double, max: Double, start: Int, end: Int) extends Filter {

  override def filter(): List[String] = {

    val map = mutable.Map[String, Int]()
    val jedis = RedisHandler.getInstance().getJedis

    for (i <- start to end) {

      val key = prefix + CommonUtil.getDateStr(i)
      val result = jedis.zrangeByScore(key, min, max)

      val iterator = result.iterator()

      while (iterator.hasNext) {
        val code = iterator.next()
        map.put(code, map.getOrElse(code, 0) + 1)
      }

    }

    jedis.close()

    map.filter((x) => x._2 >= min && x._2 <= max).keys.toList
  }

}

object SumValueFilter {

  def apply(prefix: String, min: Double, max: Double, start: Int, end: Int): SumValueFilter = {

    val filter = new SumValueFilter(prefix, min, max, start, end)

    filter.futureTask = new FutureTask[List[String]](new Callable[List[String]] {
      override def call(): List[String] = filter.filter()
    })

    filter
  }

}
