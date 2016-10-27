package com.kunyandata.backtesting.filter

import java.util.concurrent.{Callable, FutureTask}

import com.kunyandata.backtesting.io.RedisHandler
import com.kunyandata.backtesting.util.CommonUtil

import scala.collection.mutable

/**
  * Created by yang
  * Created on 9/6/16.
  */
class SimpleUnionFilter private(prefix: String, event: String, start: Int, end: Int) extends Filter {

  override def filter(): List[String] = {

    var resultSet = mutable.Set[String]()
    val jedis = RedisHandler.getInstance().getJedis

    for (i <- start to end) {

      val key = prefix + CommonUtil.getDateStr(i)
      val value = jedis.hget(key, event)
      if (value != null)
        resultSet ++= value.split(",").toSet
    }

    jedis.close()
    resultSet.toList
  }

}

object SimpleUnionFilter {

  def apply(key: String, event: String, start: Int, end: Int): SimpleUnionFilter = {

    val filter = new SimpleUnionFilter(key, event, start, end)

    filter.futureTask = new FutureTask[List[String]](new Callable[List[String]] {
      override def call(): List[String] = filter.filter()
    })

    filter
  }

}
