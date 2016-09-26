package com.kunyandata.backtesting.parser

/**
  * Created by QQ on 2016/8/30.
  */
object Rules {

  /**
    * 获取条件语句中的数值（带百分号）
    * @param query 条件语句
    * @return Array[String]
    */
  def getNumbers(query: String): Array[String] = {

    val temp = query
      .replaceAll("[\\u4e00-\\u9fa5]", " ")
      .replaceAll("[a-zA-Z~]", " ")
      .replaceAll("[（）]", "")
      .trim.split(" ")

    temp.filter(_.length > 0)
  }

  /**
    * 大于类型数据转化为区间表示
    * @param number 数值
    * @return
    */
  def bigger(number: String): String = {

    number.contains("%") match {

      case true => s"${number.replaceAll("%", "").toDouble},${Int.MaxValue}" // 保留百分比的解析
      case _ => s"$number,${Int.MaxValue}"
    }
  }
  /**
    * 小于类型数据转化为区间表示
    * @param number 数值
    * @return
    */
  def smaller(number: String): String = {

    number.contains("%") match {

      case true => s"${Int.MinValue},${number.replaceAll("%", "").toDouble}" // 保留百分比的解析
      case _ => s"${Int.MinValue},$number"
    }
  }
  /**
    * 等于类型数据转化为区间表示
    * @param number 数值
    * @return
    */
  def equel(number: String): String = {

    number.contains("%") match {

      case true => s"${number.replaceAll("%", "").toDouble}" +
        s",${number.replaceAll("%", "").toDouble}"  // 保留百分比的解析
      case _ => s"$number,$number"
    }
  }

  /**
    * 大于且小于类型数据转化为区间表示，并且判断数值大小逻辑关系
    * @param number 数值
    * @return
    */
  def biggerAndSmaller(number:Array[String]): String = {

    number.length match {

      case 2 =>

        val nums = number.map(num => {

          num.contains("%") match {

            case true => num.replaceAll("%", "").toDouble // 保留百分比的解析
            case _ => num.toDouble
          }
        })

        nums(0) <= nums(1) match {

          case true => s"${nums(0)},${nums(1)}"
          case _ => "error:数值大小关系错误"
        }

      case _ =>  "error:条件数值个数错误"
    }
  }

  /**
    * 解析方法
    * @param query 条件语句
    * @return
    */
  def template(query: String): (Int, String) = {

    val queryNumbers = getNumbers(query)
    var queryTemplate: String = query

    queryNumbers.foreach(num => {

      queryTemplate = queryTemplate.replaceFirst(num.replace("%", ""), "x")
    })

    val resultTemp = queryTemplate match {

      // 基本面数据
      case "总股本大于x万" => (101, bigger(queryNumbers(0)))
      case "总股本小于x万" => (101, smaller(queryNumbers(0)))
      case "总股本等于x万" => (101, equel(queryNumbers(0)))
      case "总股本大于x万小于x万" => (101, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "流通股本大于x万" => (102, bigger(queryNumbers(0)))
      case "流通股本小于x万" => (102, smaller(queryNumbers(0)))
      case "流通股本等于x万" => (102, equel(queryNumbers(0)))
      case "流通股本大于x万小于x万" => (102, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "总市值大于x万" => (103, bigger(queryNumbers(0)))
      case "总市值小于x万" => (103, smaller(queryNumbers(0)))
      case "总市值等于x万" => (103, equel(queryNumbers(0)))
      case "总市值大于x万小于x万" => (103, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "流通市值大于x万" => (104, bigger(queryNumbers(0)))
      case "流通市值小于x万" => (104, smaller(queryNumbers(0)))
      case "流通市值等于x万" => (104, equel(queryNumbers(0)))
      case "流通市值大于x万小于x万" => (104, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "流通比例大于x%" => (105, bigger(queryNumbers(0)))
      case "流通比例小于x%" => (105, smaller(queryNumbers(0)))
      case "流通比例等于x%" => (105, equel(queryNumbers(0)))
      case "流通比例大于x%小于x%" => (105, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "十大股东持股比例大于x%" => (106, bigger(queryNumbers(0)))
      case "十大股东持股比例小于x%" => (106, smaller(queryNumbers(0)))
      case "十大股东持股比例等于x%" => (106, equel(queryNumbers(0)))
      case "十大股东持股比例大于x%小于x%" => (106, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "股东户数大于x万" => (107, bigger(queryNumbers(0)))
      case "股东户数小于x万" => (107, smaller(queryNumbers(0)))
      case "股东户数等于x万" => (107, equel(queryNumbers(0)))
      case "股东户数大于x万小于x万" => (107, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "户均持股数大于x万" => (108, bigger(queryNumbers(0)))
      case "户均持股数小于x万" => (108, smaller(queryNumbers(0)))
      case "户均持股数等于x万" => (108, equel(queryNumbers(0)))
      case "户均持股数大于x万小于x万" => (108, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "机构持股数大于x万" => (109, bigger(queryNumbers(0)))
      case "机构持股数小于x万" => (109, smaller(queryNumbers(0)))
      case "机构持股数等于x万" => (109, equel(queryNumbers(0)))
      case "机构持股数大于x万小于x万" => (109, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "高管增股" => (40001, "高管增股")
      case "高管减股" => (40001, "高管减股")

      // 技术面数据
      case "涨跌幅大于x%" => (201, bigger(queryNumbers(0)))
      case "涨跌幅小于x%" => (201, smaller(queryNumbers(0)))
      case "涨跌幅等于x%" => (201, equel(queryNumbers(0)))
      case "涨跌幅大于x%小于x%" => (201, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "涨幅大于x%" => (202, bigger(queryNumbers(0)))
      case "涨幅小于x%" => (202, smaller(queryNumbers(0)))
      case "涨幅等于x%" => (202, equel(queryNumbers(0)))
      case "涨幅大于x%小于x%" => (202, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "跌幅大于x%" => (203, bigger(queryNumbers(0)))
      case "跌幅小于x%" => (203, smaller(queryNumbers(0)))
      case "跌幅等于x%" => (203, equel(queryNumbers(0)))
      case "跌幅大于x%小于x%" => (203, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "振幅大于x%" => (204, bigger(queryNumbers(0)))
      case "振幅小于x%" => (204, smaller(queryNumbers(0)))
      case "振幅等于x%" => (204, equel(queryNumbers(0)))
      case "振幅大于x%小于x%" => (204, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "换手率大于x%" => (205, bigger(queryNumbers(0)))
      case "换手率小于x%" => (205, smaller(queryNumbers(0)))
      case "换手率等于x%" => (205, equel(queryNumbers(0)))
      case "换手率大于x%小于x%" => (205, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "成交量大于x万" => (206, bigger(queryNumbers(0)))
      case "成交量小于x万" => (206, smaller(queryNumbers(0)))
      case "成交量等于x万" => (206, equel(queryNumbers(0)))
      case "成交量大于x万小于x万" => (206, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "成交额大于x万" => (207, bigger(queryNumbers(0)))
      case "成交额小于x万" => (207, smaller(queryNumbers(0)))
      case "成交额等于x万" => (207, equel(queryNumbers(0)))
      case "成交额大于x万小于x万" => (207, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "股价大于x元" => (208, bigger(queryNumbers(0)))
      case "股价小于x元" => (208, smaller(queryNumbers(0)))
      case "股价等于x元" => (208, equel(queryNumbers(0)))
      case "股价大于x元小于x元" => (208, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "收益率大于x%" => (209, bigger(queryNumbers(0)))
      case "收益率小于x%" => (209, smaller(queryNumbers(0)))
      case "收益率等于x%" => (209, equel(queryNumbers(0)))
      case "收益率大于x%小于x%" => (209, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "日均查看热度离均差（MAx）大于x倍前x天日均热度标准差" => (210, s"${queryNumbers(1)},${queryNumbers(0)},${queryNumbers(2)}")

      case "资金流入大于x万" => (301, bigger(queryNumbers(0)))
      case "资金流入小于x万" => (301, smaller(queryNumbers(0)))
      case "资金流入等于x万" => (301, equel(queryNumbers(0)))
      case "资金流入大于x万小于x万" => (301, biggerAndSmaller(queryNumbers.slice(0, 2)))

      // 消息面数据
      case "新闻访问热度每天大于x次" => (401, bigger(queryNumbers(0)))
      case "新闻访问热度每天小于x次" => (401, smaller(queryNumbers(0)))
      case "新闻访问热度每天等于x次" => (401, equel(queryNumbers(0)))
      case "新闻访问热度每天大于x次小于x次" => (401, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "新闻访问热度每周大于x次" => (2401, bigger(queryNumbers(0)))
      case "新闻访问热度每周小于x次" => (2401, smaller(queryNumbers(0)))
      case "新闻访问热度每周等于x次" => (2401, equel(queryNumbers(0)))
      case "新闻访问热度每周大于x次小于x次" => (2401, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "新闻访问热度每月大于x次" => (4401, bigger(queryNumbers(0)))
      case "新闻访问热度每月小于x次" => (4401, smaller(queryNumbers(0)))
      case "新闻访问热度每月等于x次" => (4401, equel(queryNumbers(0)))
      case "新闻访问热度每月大于x次小于x次" => (4401, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "新闻访问热度每年大于x次" => (8401, bigger(queryNumbers(0)))
      case "新闻访问热度每年小于x次" => (8401, smaller(queryNumbers(0)))
      case "新闻访问热度每年等于x次" => (8401, equel(queryNumbers(0)))
      case "新闻访问热度每年大于x次小于x次" => (8401, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "新闻转载热度每天大于x次" => (402, bigger(queryNumbers(0)))
      case "新闻转载热度每天小于x次" => (402, smaller(queryNumbers(0)))
      case "新闻转载热度每天等于x次" => (402, equel(queryNumbers(0)))
      case "新闻转载热度每天大于x次小x次" => (402, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "新闻转载热度每周大于x次" => (2402, bigger(queryNumbers(0)))
      case "新闻转载热度每周小于x次" => (2402, smaller(queryNumbers(0)))
      case "新闻转载热度每周等于x次" => (2402, equel(queryNumbers(0)))
      case "新闻转载热度每周大于x次小于x次" => (2402, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "新闻转载热度每月大于x次" => (4402, bigger(queryNumbers(0)))
      case "新闻转载热度每月小于x次" => (4402, smaller(queryNumbers(0)))
      case "新闻转载热度每月等于x次" => (4402, equel(queryNumbers(0)))
      case "新闻转载热度每月大于x次小于x次" => (4402, biggerAndSmaller(queryNumbers.slice(0, 2)))

      case "新闻转载热度每年大于x次" => (8402, bigger(queryNumbers(0)))
      case "新闻转载热度每年小于x次" => (8402, smaller(queryNumbers(0)))
      case "新闻转载热度每年等于x次" => (8402, equel(queryNumbers(0)))
      case "新闻转载热度每年大于x次小于x次" => (8402, biggerAndSmaller(queryNumbers.slice(0, 2)))

      //公告性事件
      case "盈利预增x次" => (50001, equel(queryNumbers(0)))
      case "诉讼仲裁x次" => (50002, equel(queryNumbers(0)))
      case "违规处罚x次" => (50003, equel(queryNumbers(0)))
      case "盈利预增x次以上" => (50001, bigger(queryNumbers(0)))
      case "诉讼仲裁x次以上" => (50002, bigger(queryNumbers(0)))
      case "违规处罚x次以上" => (50003, bigger(queryNumbers(0)))

      //新闻趋势
      case "新闻趋势连续x天上涨" => (10001, s"${queryNumbers(0)},1,1")
      case "新闻趋势连续x天下降" => (10002, s"${queryNumbers(0)},0,0")
      case "新闻趋势连续x天以上上涨" => (10001, s"${queryNumbers(0)},1,1")
      case "新闻趋势连续x天以上下降" => (10002, s"${queryNumbers(0)},0,0")

      //新闻情感
      case "新闻情感连续x天都是非负面情绪" => (10003, s"${queryNumbers(0)},0,0.5")
      case "新闻情感连续x天都是负面情绪" => (10004, s"${queryNumbers(0)},0.5,1")
      case "新闻情感连续x天以上都是非负面情绪" => (10003, s"${queryNumbers(0)},0,0.5")
      case "新闻情感连续x天以上都是负面情绪" => (10004, s"${queryNumbers(0)},0.5,1")

      //大V观点
      case "连续x天被x个大V看好" => (15001, s"${queryNumbers(0)},${queryNumbers(1)},${queryNumbers(1)}")
      case "连续x天被x个大V看空" => (15002, s"${queryNumbers(0)},${queryNumbers(1)},${queryNumbers(1)}")
      case "连续x天以上被x个大V看好" => (15001, s"${queryNumbers(0)},${queryNumbers(1)},${queryNumbers(1)}")
      case "连续x天以上被x个大V看空" => (15002, s"${queryNumbers(0)},${queryNumbers(1)},${queryNumbers(1)}")
      case "连续x天被x个大V以上看好" => (15001, s"${queryNumbers(0)},${queryNumbers(1)},${Int.MaxValue}")
      case "连续x天被x个大V以上看空" => (15002, s"${queryNumbers(0)},${queryNumbers(1)},${Int.MaxValue}")
      case "连续x~x天被x个大V看好" => (15001, s"${queryNumbers(0)},${queryNumbers(2)},${queryNumbers(2)}")
      case "连续x~x天被x个大V看空" => (15002, s"${queryNumbers(0)},${queryNumbers(2)},${queryNumbers(2)}")
      case "连续x天被x~x个大V看好" => (15001, s"${queryNumbers(0)},${queryNumbers(1)},${queryNumbers(2)}")
      case "连续x天被x~x个大V看空" => (15002, s"${queryNumbers(0)},${queryNumbers(1)},${queryNumbers(2)}")

      //行为数据
      case "查看热度连续x天上涨超过x" => (15003, s"${queryNumbers(0)},${queryNumbers(1)},${Int.MaxValue}")
      case "查看热度连续x天出现在topx" => (15004, s"${queryNumbers(0)},1,${queryNumbers(1)}")
      case "查看热度连续x天以上上涨超过x" => (15003, s"${queryNumbers(0)},${queryNumbers(1)},${Int.MaxValue}")
      case "查看热度连续x天以上出现在topx" => (15004, s"${queryNumbers(0)},1,${queryNumbers(1)}")
      case "查看热度连续x天超过x" => (15005, s"${queryNumbers(0)},${queryNumbers(1)},${Int.MaxValue}")
      case "查看热度连续x天以上超过x" => (15005, s"${queryNumbers(0)},${queryNumbers(1)},${Int.MaxValue}")

      case _ => (-1, query)
    }

    resultTemp._2.startsWith("error:") match {

      case true => (-1, query)
      case _ => resultTemp
    }
  }
}
