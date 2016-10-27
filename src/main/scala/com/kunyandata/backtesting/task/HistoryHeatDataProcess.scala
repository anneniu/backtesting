package com.kunyandata.backtesting.task
import java.util
import com.kunyandata.backtesting.logger.BKLogger
import com.kunyandata.backtesting.util.CommonUtil
import redis.clients.jedis.{Jedis, Tuple}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.util.control.Breaks

/**
  * Created by dcyang on 2016/10/20 0020.
  */
object HistoryHeatDataProcess {

  /**
    * 把redis读取的结果转换成Map[value, score]
    *
    * @param set redis 读取的股票热度数据
    * @return 转换后的Map
    */
  def valueAndScoreToMap(set: java.util.Set[Tuple]): mutable.Map[String, Double] = {

    val resultMap = mutable.Map[String, Double]()
    val iterator = set.iterator

    while (iterator.hasNext) {
      val valueAndScore = iterator.next()
      resultMap.put(valueAndScore.getElement, valueAndScore.getScore)
    }

    resultMap
  }

  /**
    * 计算均值，标准差
    *
    * @param arrayBuffer 输入为历史热度的ArrayBuffer
    * @return 均值、标准差
    * @author dcyang
    * @note rowNum = 9
    */
  def getMeanAndStd(arrayBuffer: ArrayBuffer[Double]): (Double, Double) = {

    val num = arrayBuffer.size

    if (num == 0 || num == 1) (-1.0, -1.0)
    else {
      val mean = arrayBuffer.sum / num
      val std = Math.sqrt(arrayBuffer.map(x => Math.pow(x - mean, 2)).sum / (num -1))

      (mean, std)
    }

  }

  /**
    * 获取当天之前的offDay（eg: 5）个有效的历史日期
    *
    * @param jedis redis client
    * @param nowDate 当天日期
    * @param offDay 有效历史天数
    * @return 有效历史日期数据 Array[日期]
    * @author dcyang
    * @note rowNum = 21
    */
  def getPreDates(jedis: Jedis, nowDate: String, offDay: Int): Array[String] = {

    val preDates = new ArrayBuffer[String]()
    val prefix = "count_heat_"

    val loop = new Breaks
    loop.breakable{

      var lastDate = nowDate
      while(preDates.size < offDay) {

        val tempDate = CommonUtil.getDateStr(lastDate, -1)
        val tempKey = prefix + tempDate
        val stockNum = jedis.zcard(tempKey)

        if (stockNum > 2500){
          preDates.+=(tempDate)
        }else{

          if (stockNum == 0){
            BKLogger.warn("缺失数据：" + "\t" + tempDate)
          }else{
            BKLogger.warn("记录少于2500" + "\t" + tempDate)
          }

        }

        if (tempDate.equals("2016-01-06")){
          loop.break()
        }

        lastDate = tempDate
      }

    }

    if(preDates.nonEmpty){
      BKLogger.info("有效历史日期" + "\t" + "start：" + preDates.apply(0) + "\t" + "end：" + preDates.last + "\t"
        + "num：" + preDates.size)
    }

    preDates.toArray
  }

  /**
    * 往股票代码和历史热度数据的Map里填充数据
    *
    * @param jedis redis客户端
    * @param preDates 有效历史日期集合
    * @param initialMap 初始股票代码和历史热度数据的Map
    * @return Map[股票代码， Array[热度数据] ]
    * @author dcyang
    * @note rowNum = 20
    */
  def getCodeHeatArrayMap(jedis: Jedis, preDates: Array[String],
                          initialMap: mutable.Map[String, ArrayBuffer[Double]]) ={

    val prefix = "count_heat_"
    val start = Int.MinValue
    val end = Int.MaxValue
    val map = initialMap

    for (preDay <- preDates) {

      val preKey = prefix + preDay
      val codesWithScores: util.Set[Tuple] = jedis.zrangeWithScores(preKey, start, end)
      val codeScoreMap = valueAndScoreToMap(codesWithScores)

      val codes: collection.Set[String] = map.keySet

      val iterator: Iterator[String] = codes.iterator

      while(iterator.hasNext){

        val code = iterator.next()

        if (codeScoreMap.contains(code)) {
          map(code).+=(codeScoreMap(code))
        }

      }

    }

    map
  }

  /**
    * 初始化股票代码和历史热度数据的Map
    *
    * @param date 当天日期
    * @param jedis redis 客户端
    * @return Map[股票代码， ArrayBuffer[空] ]
    * @author dcyang
    * @note rowNum = 14
    */
  def initCodeHeatArrayMap(date: String, jedis: Jedis) = {

    val prefix = "count_heat_"
    val key = prefix + date
    val start = Int.MinValue
    val end = Int.MaxValue
    val initialMap = mutable.Map[String, ArrayBuffer[Double]]()
    val codes: util.Set[String] = jedis.zrange(key, start, end)

    val iterator = codes.iterator()

    while(iterator.hasNext){
      val code = iterator.next()
      initialMap.put(code, new ArrayBuffer[Double]())
    }

    initialMap
  }

  /**
    * 均值和方差数据写入redis
    *
    * @param codeWithMeanAndStd Map[股票代码, (均值, 方差)
    * @param jedis redis 客户端
    * @param outMeanKey 输出均值key
    * @param outStdKey 输出方差key
    * @author dcyang
    * @note rowNum = 16
    */
  def meanStdWriteToJedis(codeWithMeanAndStd: mutable.Map[String, (Double, Double)],
                          jedis: Jedis, outMeanKey: String, outStdKey: String) {

    val iterator = codeWithMeanAndStd.iterator
    while(iterator.hasNext){

      val codeMeanStd = iterator.next()
      val code = codeMeanStd._1
      val mean = codeMeanStd._2._1
      val std = codeMeanStd._2._2

      if(mean != -1.0){
        jedis.zadd(outMeanKey, mean, code)
      }

      if(std != -1.0){
        jedis.zadd(outStdKey, std, code)
      }

    }
  }

  /**
    * 将Map[股票代码， 股票热度] 转换成Map[行业， 行业热度]
    *
    * @param codeScoreMap  Map[股票代码， 股票热度]
    * @param codeIndustryMap Map[股票代码， 所属行业]
    * @return Map[行业， 行业热度]
    * @author dcyang
    * @note rowNum = 16
    */
  def codeScoreToIndScore(codeScoreMap: mutable.Map[String, Double],
                          codeIndustryMap: mutable.Map[String, String]): mutable.Map[String, Double] ={

    val indAndHeatsMap = mutable.Map[String, ArrayBuffer[Double]]()

    codeScoreMap.map(x => {

      val code = x._1
      val heat = x._2
      if (codeIndustryMap.contains(code)){

        val industry = codeIndustryMap(code)
        if (indAndHeatsMap.contains(industry)){
          indAndHeatsMap(industry).+=(heat)
        }else{
          indAndHeatsMap.put(industry, new ArrayBuffer[Double]().+= (heat))
        }

      }

    })

    indAndHeatsMap.map(x => (x._1, x._2.sum))
  }

  /**
    * 从本地文件获取股票行业的对应关系
    *
    * @param filePath  股票行业的对应关系文件地址
    * @return Map[股票代码， 所属行业]
    * @author dcyang
    * @note rowNum = 14
    */
  def getStockIndustryMap(filePath: String): mutable.Map[String, String] ={

    val stockIndustryMap: mutable.Map[String, String] = mutable.Map[String, String]()
    val stockAndIndustry: Iterator[(String, String)] = Source.fromFile(filePath).getLines().map(line => {

      val fields = line.split("\t")
      val stock = fields(0)
      val industry = fields(1)

      (stock, industry)
    })

    while(stockAndIndustry.hasNext){
      val stockAndInd = stockAndIndustry.next()
      stockIndustryMap.put(stockAndInd._1, stockAndInd._2)
    }

    stockIndustryMap
  }

  /**
    * 初始化 行业与历史行业热度的Map
    *
    * @param date 当前日期
    * @param jedis redis 客户端
    * @param stockIndustryMap Map[股票代码， 所属行业]
    * @return Map[股票代码，ArrayBuffer[空] ]
    * @author dcyang
    * @note rowNum = 19
    */
  def initIndustryHeatArrayMap(date: String, jedis: Jedis, stockIndustryMap: mutable.Map[String, String]) ={

    val prefix = "count_heat_"
    val key = prefix + date
    val start = Int.MinValue
    val end = Int.MaxValue
    val map = mutable.Map[String, ArrayBuffer[Double]]()
    val codes = jedis.zrange(key, start, end)

    val iterator = codes.iterator()

    while(iterator.hasNext){

      val code = iterator.next()
      if(stockIndustryMap.contains(code)){
        val industry = stockIndustryMap(code)
        if (!map.contains(industry)) {
          map.put(industry, new ArrayBuffer[Double]())
        }
      }

    }

    map
  }

  /**
    * 获取行业热度历史数据
    *
    * @param jedis redis 客户端
    * @param preDates 有效历史日期集合
    * @param initialMap  Map[股票代码，ArrayBuffer[空] ]
    * @param stockIndustryMap Map[股票代码， 所属行业]
    * @return Map[行业，ArrayBuffer[历史行业热度] ]
    * @author dcyang
    * @note rowNum = 21
    */
  def getIndustryHeatArrayMap(jedis: Jedis, preDates: Array[String],
                              initialMap: mutable.Map[String, ArrayBuffer[Double]],
                              stockIndustryMap: mutable.Map[String, String]) ={

    val prefix = "count_heat_"
    val start = Int.MinValue
    val end = Int.MaxValue
    val map = initialMap

    for (preDate <-  preDates) {

      val preKey = prefix + preDate
      val codesWithScores: util.Set[Tuple] = jedis.zrangeWithScores(preKey, start, end)
      val codeScoreMap = HistoryHeatDataProcess.valueAndScoreToMap(codesWithScores)
      val industryScoreMap = HistoryHeatDataProcess.codeScoreToIndScore(codeScoreMap, stockIndustryMap)

      val industryIterator = map.keysIterator

      while(industryIterator.hasNext){

        val industry = industryIterator.next()

        if (industryScoreMap.contains(industry)) {
          map(industry).+=(industryScoreMap(industry))
        }

      }

    }

    map
  }

}
