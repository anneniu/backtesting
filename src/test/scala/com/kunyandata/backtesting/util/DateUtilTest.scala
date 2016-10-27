package com.kunyandata.backtesting.util

import org.scalatest.{Matchers, FlatSpec}

/**
  * Created by YangShuai
  * Created on 2016/8/31.
  */
class DateUtilTest extends FlatSpec with Matchers {

  it should "return a negative integer represents the offset between the target date and today" in {

    val dateString = "2016-08-29"
    val offset = CommonUtil.getOffset(dateString)
    println(s"the offset between $dateString and today is $offset")

  }

}
