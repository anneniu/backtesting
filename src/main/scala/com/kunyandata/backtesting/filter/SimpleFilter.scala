package com.kunyandata.backtesting.filter

import java.util.concurrent.{Callable, FutureTask}

import com.kunyandata.backtesting.io.RedisHandler

/**
  * Created by YangShuai
  * Created on 2016/9/12.
  */
class SimpleFilter private(key: String, field: String) extends Filter {

  override def filter(): List[String] = {

    val jedis = RedisHandler.getInstance().getJedis
    val result = jedis.hget(key, field)

    jedis.close()
    result.split(",").toList
  }

}

object SimpleFilter {

  def apply(key: String, field: String): SimpleFilter = {

    val filter = new SimpleFilter(key, field)

    filter.futureTask = new FutureTask[List[String]](new Callable[List[String]] {
      override def call(): List[String] = filter.filter()
    })

    filter
  }
}