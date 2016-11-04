package com.kunyandata.backtesting.task
import java.text.SimpleDateFormat
import java.util
import com.kunyandata.backtesting.io.RedisHandler
import com.kunyandata.backtesting.logger.BKLogger
import redis.clients.jedis.Tuple
import scala.collection.mutable


/**
  * Created by Administrator on 2016/10/11 0011.
  */
object IndustryHeatOneDay {

  def main(args: Array[String]) {

    // 初始化jedis
    val host = args(0)
    val port = args(1).toInt
    val auth = args(2)
    val db = args(3).toInt

    RedisHandler.init(host, port, auth, db)
    val jedis = RedisHandler.getInstance().getJedis

    // 获取股票以及行业对应关系 Map[股票代码， 所属行业]
    val stockIndustryPath = args(4)
    val stockIndustryMap = HistoryHeatDataProcess.getStockIndustryMap(stockIndustryPath)

    val prefix = "count_heat_"

    // 指定日期或默认昨日（用于定时任务）
    val date = if (args.length == 5) {

      // 获取前一天时间
      val DATE_FORMAT = "yyyy-MM-dd"
      val timeStamp = System.currentTimeMillis() - 24l * 60 * 60 * 1000

      new SimpleDateFormat(DATE_FORMAT).format(timeStamp)
    } else args(5)

    val key = prefix + date

    val start = Int.MinValue
    val end = Int.MaxValue

    try {

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

    }catch {
      case e: Exception => e.printStackTrace()
    }

    jedis.close()
  }
}
