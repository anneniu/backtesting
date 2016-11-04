package com.kunyandata.backtesting.task
import com.kunyandata.backtesting.io.RedisHandler
import com.kunyandata.backtesting.logger.BKLogger
import com.kunyandata.backtesting.util.CommonUtil
import scala.collection.mutable

/**
  * Created by Administrator on 2016/9/29 0029.
  */
object IndustryHeatMeanAndStdHistory {

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
    val stockIndustryMap: mutable.Map[String, String] = HistoryHeatDataProcess.getStockIndustryMap(stockIndustryPath)

    // 指定开始日期
    val initDate = args(5)

    // 指定需要计算的时间跨度（如：5天、10天）
    val offDay = args(6).toInt

    val endDate = CommonUtil.getDateStr("2016-01-06", offDay)
    val dayNums = CommonUtil.getOffset(initDate, endDate)

    try {

      for(i <- 0 to dayNums){

        val date = CommonUtil.getDateStr(initDate,-i)

        val key = "count_heat_" + date
        val stockNum = jedis.zcard(key)

        if (stockNum != 0){

          // 初始化行业与历史行业热度的Map，Map[行业， ArrayBuffer[空]]
          val initialMap = HistoryHeatDataProcess.initIndustryHeatArrayMap(date, jedis, stockIndustryMap)

          // 获取有效的offDay（如5）个有效日期
          val preDates = HistoryHeatDataProcess.getPreDates(jedis, date, offDay)

          if (preDates.length == offDay){

            // 获取行业与历史行业热度的Map， Map[行业， ArrayBuffer[历史行业热度]]
            val industryHeatArrayMap = HistoryHeatDataProcess
              .getIndustryHeatArrayMap(jedis, preDates, initialMap, stockIndustryMap)

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
