package com.kunyandata.backtesting.task
import java.text.SimpleDateFormat
import com.kunyandata.backtesting.io.RedisHandler
import com.kunyandata.backtesting.logger.BKLogger

/**
  * Created by dcyang on 2016/9/29 0029.
  */
object IndustryHeatMeanAndStdOneDay {

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

    // 指定日期或默认昨日（用于定时任务）
    val date = if (args.length == 5) {

      // 获取前一天时间
      val DATE_FORMAT = "yyyy-MM-dd"
      val timeStamp = System.currentTimeMillis() - 24l * 60 * 60 * 1000

      new SimpleDateFormat(DATE_FORMAT).format(timeStamp)
    } else args(5)

    // 指定需要计算的时间跨度（如：5天、10天）
    val offDays = List(5, 7, 10, 14, 15, 20, 30, 60)

    try {

      val prefix = "count_heat_"
      val key = prefix + date
      val stockNum = jedis.zcard(key)

      if (stockNum != 0){

        for(offDay <- offDays){

          // 初始化行业与历史行业热度的Map，Map[行业， ArrayBuffer[空]]
          val initialMap = HistoryHeatDataProcess.initIndustryHeatArrayMap(date, jedis, stockIndustryMap)

          // 获取有效的offDay（如5）个有效日期
          val preDates = HistoryHeatDataProcess.getPreDates(jedis, date, offDay)

          if (preDates.length == offDay){

            // 获取行业与历史行业热度的Map， Map[行业， ArrayBuffer[历史行业热度]]
            val industryHeatArrayMap =
              HistoryHeatDataProcess.getIndustryHeatArrayMap(jedis, preDates, initialMap, stockIndustryMap)

            // 计算均值方差
            val industryWithMeanAndStd = industryHeatArrayMap
              .map(x => (x._1, HistoryHeatDataProcess.getMeanAndStd(x._2)))

            // 写入redis
            val outMeanKey = "industry_heat_mean_" + offDay + "_" + date
            val outStdKey = "industry_heat_std_" + offDay + "_" + date
            HistoryHeatDataProcess.meanStdWriteToJedis(industryWithMeanAndStd, jedis, outMeanKey, outStdKey)

            println(date + "\t" + offDay)

          }else{
            BKLogger.info(date + "\t" + "have not sufficient historyData")
          }

        }

      }else{
        BKLogger.warn("缺失数据：" + date)
      }

    }catch {
      case e: Exception => e.printStackTrace()
    }

    jedis.close()

  }
}
