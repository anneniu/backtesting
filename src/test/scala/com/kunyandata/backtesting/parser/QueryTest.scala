package com.kunyandata.backtesting.parser

import com.kunyandata.backtesting.config.FilterType
import org.scalatest.{Matchers, FlatSpec}

/**
  * Created by YangShuai
  * Created on 2016/8/30.
  */
class QueryTest extends FlatSpec with Matchers {

  it should "change an option string to a map" in {

    val str = "4:新闻情感连续7天都是负面情绪"
    val map = Query.parse(str)

    map.keySet.foreach(key => {

      println(FilterType.apply(key).toString)
      map(key) should be("7,0.5,1")
    })

  }

}
