package com.kunyandata.backtesting.task
import java.util
import com.kunyandata.backtesting.io.RedisHandler
import com.kunyandata.backtesting.logger.BKLogger
import com.kunyandata.backtesting.util.CommonUtil
import redis.clients.jedis.Tuple
import scala.collection.mutable

/**
  * 计算历史行业热度
  * Created by dcyang on 2016/10/11 0011.
  */
object IndustryHeatHistory {

  def main(args: Array[String]) {

    // 初始化jedis
    val host = args(0)
    val port = args(1).toInt
    val auth = args(2)
    val db = args(3).toInt

    RedisHandler.init(host, port, auth, db)
    val jedis = RedisHandler.getInstance().getJedis

    val stockIndustryPath = args(4)
    val stockIndustryMap = HistoryHeatDataProcess.getStockIndustryMap(stockIndustryPath)

    val prefix = "count_heat_"

    // 指定开始日期
    val initDate = args(5)
    val endDate = "2016-01-06"
    val dayNums = CommonUtil.getOffset(initDate, endDate)

    try {

      for (i <- 0 to dayNums){

        // 获取当天日期
        val date = CommonUtil.getDateStr(initDate,-i)
        val key = prefix + date
        val start = Int.MinValue
        val end = Int.MaxValue
        val codesWithScores: util.Set[Tuple] = jedis.zrangeWithScores(key, start, end)

        if (!codesWithScores.isEmpty){

          // 获取 Map[股票， 股票热度]
          val codeScoreMap = HistoryHeatDataProcess.valueAndScoreToMap(codesWithScores)

          // 获取 Map[行业， 行业热度]
          val industryScoreMap: mutable.Map[String, Double] =
            HistoryHeatDataProcess.codeScoreToIndScore(codeScoreMap, stockIndustryMap)

          // 写入redis
          val outKey = "industry_heat_" + date

          if(industryScoreMap.nonEmpty){
            industryScoreMap.map(x => {

              val industry = x._1
              val heat = x._2
              jedis.zadd(outKey, heat, industry)

            })
          }
          println(outKey)

        }else{
          BKLogger.warn("缺失数据：" + date)
        }
      }
    }catch {
      case e: Exception => e.printStackTrace()
    }

    jedis.close()
  }
}
