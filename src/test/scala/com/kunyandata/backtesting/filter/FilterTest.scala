package com.kunyandata.backtesting.filter

import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by YangShuai
  * Created on 2016/9/1.
  */
class FilterTest extends FlatSpec with Matchers {

  it should "" in {

    /*val valueFilter = ContiValueFilter("count_heat_", 1000, Integer.MAX_VALUE, 10, -10, -1)
    val diffFilter = ContiValueFilter("diff_heat_", 10, Integer.MAX_VALUE, 3, -5, -1)
    val rankFilter = ContiRankFilter("count_heat_", 100, 3, -5, -1)

    threadPool.execute(valueFilter.getFutureTask)
    threadPool.execute(diffFilter.getFutureTask)
    threadPool.execute(rankFilter.getFutureTask)

    val valueList = valueFilter.getResult
    val diffList = diffFilter.getResult
    val rankList = rankFilter.getResult

    println("value size: " + valueList.size)
    println("diff size: " + diffList.size)
    println("rank size: " + rankList.size)

    valueList.intersect(diffList).intersect(rankList).mkString(",")*/

  }

}
