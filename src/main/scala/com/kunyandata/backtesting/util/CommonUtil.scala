package com.kunyandata.backtesting.util

import java.text.{ParseException, SimpleDateFormat}

import com.kunyandata.backtesting.logger.BKLogger


/**
  * Created by YangShuai
  * Created on 2016/8/24.
  */
object CommonUtil {

  val DATE_FORMAT = "yyyy-MM-dd"

  /**
    * 获得yyyy-MM-dd格式的代表日期的字符串
    *
    * @param offset 偏差值，如果需获得昨天的日期字符串则传-1
    * @return
    */
  def getDateStr(offset: Int): String = {

    val timeStamp = System.currentTimeMillis() + offset * 24l * 60 * 60 * 1000

    new SimpleDateFormat(DATE_FORMAT).format(timeStamp)
  }

  /**
    * 获取距离指定天偏差天数的日期字符串
    * @param date 指定日期字符串
    * @param offset 偏差天数
    * @return yyyy-MM-dd格式的代表日期的字符串
    */
  def getDateStr(date: String, offset: Int): String = {

    val timeStamp = new SimpleDateFormat(DATE_FORMAT).parse(date).getTime + offset * 24l * 60 * 60 * 1000

    new SimpleDateFormat(DATE_FORMAT).format(timeStamp)
  }

  /**
    * 获取给定时间字符串的时间戳
    * @param dateString 制定的时间字符串
    * @return 毫秒级的时间戳
    * @author QiuQiu
    */
  def getDateTimeStamp(dateString: String): Long = {

    new SimpleDateFormat("yyyy-M-d-H").parse(dateString).getTime
  }

  /**
    * 获得给定字符串所代表的日期与当日的偏差值
    * 例如：若给的是昨天则返回-1
    *
    * @param dateString 代表日期的字符串（"yyyy-MM-dd"）
    * @return 给定字符串所代表的日期与当日的偏差值
    */
  def getOffset(dateString: String): Int = {

    try {

      val timeStamp = new SimpleDateFormat(DATE_FORMAT).parse(dateString).getTime

      ((timeStamp - System.currentTimeMillis()) / (24l * 60 * 60 * 1000)).toInt
    } catch {

      case e: ParseException =>
        BKLogger.exception(e)

        -1
    }

  }

  /**
    * 获取两个指定日期相差的天数
    * @param startDate 开始日期
    * @param endDate 结束日期
    * @return 相差天数
    */
  def getOffset(startDate: String, endDate: String): Int = {

    try {

      val startTimeStamp = new SimpleDateFormat(DATE_FORMAT).parse(startDate).getTime
      val endTimeStamp = new SimpleDateFormat(DATE_FORMAT).parse(endDate).getTime

      ((startTimeStamp - endTimeStamp) / (24l * 60 * 60 * 1000)).toInt
    } catch {

      case e: ParseException =>
        BKLogger.exception(e)

        -1
    }

  }

}
