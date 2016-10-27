package com.kunyandata.backtesting.task
import com.kunyandata.backtesting.io.RedisHandler
import com.kunyandata.backtesting.logger.BKLogger
import com.kunyandata.backtesting.util.CommonUtil
import redis.clients.jedis.Jedis
import scala.collection.mutable

/**
  * Created by dcyang on 2016/10/21 0021.
  * 跑历史数据的股票热度均值、方差
  */
object HeatMeanAndStdHistory {

  def main(args: Array[String]) {

    // 初始化Jedis
    val host = args(0)
    val port = args(1).toInt
    val auth = args(2)
    val db = args(3).toInt
    RedisHandler.init(host, port, auth, db)
    val jedis: Jedis = RedisHandler.getInstance().getJedis

    // 指定开始日期(eg: "2016-02-10")
    val initData = args(4)

    // 指定需要计算均值方差的的时间跨度（eg: 5、7、10）
    val offDay = args(5).toInt

    // 需要计算的历史天数
    val endDate = CommonUtil.getDateStr("2016-01-06", offDay)
    val dayNums = CommonUtil.getOffset(initData, endDate)

    try{

      for(i <- 0 to dayNums){

        // 计算当日日期
        val date = CommonUtil.getDateStr(initData, -i)

        val key = "count_heat_" + date
        val stockNum = jedis.zcard(key)

        if(stockNum != 0){

          // 初始化股票和历史热度数据的Map, Map[股票代码， Array[历史热度]]
          val initialMap = HistoryHeatDataProcess.initCodeHeatArrayMap(date, jedis)

          // 获取offDay（eg: 5）个有效的历史日期
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

            println(date + "\t" + offDay)
          }else{
            BKLogger.info(date + "\t" + offDay + "\t" + "have not sufficient historyData")
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
