package com.kunyandata.backtesting.task
import java.text.SimpleDateFormat
import com.kunyandata.backtesting.io.RedisHandler
import com.kunyandata.backtesting.logger.BKLogger
import scala.collection.mutable

/**
  * 计算指定日期或昨日的历史热度均值和标准差
  * Created by dcyang on 2016/9/22 0022.
  */
object HeatMeanAndStdOneDay {

  def main(args: Array[String]) {

    // 初始化jedis
    val host = args(0)
    val port = args(1).toInt
    val auth = args(2)
    val db = args(3).toInt
    RedisHandler.init(host, port, auth, db)
    val jedis = RedisHandler.getInstance().getJedis

    // 指定日期或者取昨天日期（用于定时任务）
    val date = if (args.length == 4) {

      // 获取前一天时间
      val DATE_FORMAT = "yyyy-MM-dd"
      val timeStamp = System.currentTimeMillis() - 24l * 60 * 60 * 1000

      new SimpleDateFormat(DATE_FORMAT).format(timeStamp)
    } else args(4)

    // 指定需要计算的时间跨度
    val offDays = List(5, 7, 10, 14, 15, 20, 30, 60)

    try{

      val prefix = "count_heat_"
      val key = prefix + date
      val stockNum = jedis.zcard(key)

      if (stockNum !=0){

        for (offDay <- offDays){

          // 初始化股票和历史热度数据的Map, Map[股票代码， Array[历史热度]]
          val initialMap = HistoryHeatDataProcess.initCodeHeatArrayMap(date, jedis)

          //  获取offDay（eg: 5）个有效的历史日期
          val preDates = HistoryHeatDataProcess.getPreDates(jedis, date, offDay)

          if (preDates.length == offDay){

            // 获取股票和历史热度数据的Map, Map[股票代码， Array[历史热度]]
            val codeHistoryHeatMap = HistoryHeatDataProcess.getCodeHeatArrayMap(jedis, preDates, initialMap)

            // 根据历史热度数据计算均值、方差
            val codeWithMeanAndStd: mutable.Map[String, (Double, Double)] = codeHistoryHeatMap.map(x =>
              (x._1, HistoryHeatDataProcess.getMeanAndStd(x._2)))

            // 写入redis
            val outMeanKey = "heat_mean_" + offDay + "_" + date
            val outStdKey = "heat_std_" + offDay + "_" + date

            HistoryHeatDataProcess.meanStdWriteToJedis(codeWithMeanAndStd, jedis, outMeanKey, outStdKey)

          }else{
            BKLogger.info(date + "\t" + offDay + "\t" + "have not sufficient historyData")
          }

          println(date + "\t" + offDay)
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
