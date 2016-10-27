package com.kunyandata.backtesting.filter

import java.util.concurrent.{Callable, FutureTask}

import com.kunyandata.backtesting.io.RedisHandler
import com.kunyandata.backtesting.util.CommonUtil

import scala.collection.mutable

/**
  * 每日，每周或每月均大（小）于XXX
  * Created by YangShuai
  * Created on 2016/9/1.
  */
class AllDayValueFilter private(prefix: String, min: Double, max: Double, start: Int, end: Int) extends Filter {

  override def filter(): List[String] = {

    val resultSet = mutable.Set[String]()
    var init = false
    var count = 0
    val jedis = RedisHandler.getInstance().getJedis

    for (i <- start to end) {

      val key = prefix + CommonUtil.getDateStr(i)

      if (jedis.exists(key)) {

        val result = jedis.zrangeByScore(key, min, max)

        if (i == start + count && !init) {

          val iterator = result.iterator()

          while (iterator.hasNext) {
            val code = iterator.next()
            resultSet.add(code)
          }

          init = true

        } else {

          val iterator = resultSet.iterator

          while (iterator.hasNext) {

            val code = iterator.next()

            if (!result.contains(code))
              resultSet.remove(code)

          }

        }

      } else {

        count += 1

      }

    }

    jedis.close()

    resultSet.toList
  }
}

object AllDayValueFilter {

  def apply(prefix: String, min: Double, max: Double, start: Int, end: Int): AllDayValueFilter = {

    val filter = new AllDayValueFilter(prefix, min, max, start, end)

    filter.futureTask = new FutureTask[List[String]](new Callable[List[String]] {
      override def call(): List[String] = filter.filter()
    })

    filter
  }

}
