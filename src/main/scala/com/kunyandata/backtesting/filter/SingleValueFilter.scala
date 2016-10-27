package com.kunyandata.backtesting.filter

import java.util.concurrent.{Callable, FutureTask}

import com.kunyandata.backtesting.io.RedisHandler

import scala.collection.JavaConversions._

/**
  * 针对不需要每天（周，月）存一份的数据的过滤
  * 例如：总股本大于X亿
  * Created by YangShuai
  * Created on 2016/9/2.
  */
class SingleValueFilter private(prefix: String, min: Double, max: Double) extends Filter {

  override def filter(): List[String] = {

    val key = prefix
    val jedis = RedisHandler.getInstance().getJedis
    val result = jedis.zrangeByScore(key, min, max)

    jedis.close()

    result.toList
  }

}

object SingleValueFilter {

  def apply(prefix: String, min: Double, max: Double): SingleValueFilter = {

    val filter = new SingleValueFilter(prefix, min, max)

    filter.futureTask = new FutureTask[List[String]](new Callable[List[String]] {
      override def call(): List[String] = filter.filter()
    })

    filter
  }

}
