package com.kunyandata.backtesting.parser

/**
  * Created by QQ on 2016/8/30.
  */
object Query {

  /**
    * 查询条件解析方法
    *
    * @param query 查询条件
    * @return
    */
  def parse(query: String): Map[Int, String] = {

    val queries = query.split("\\+")

    val resultTemp = queries.map(query => {

      parseByType(query)
    }).groupBy(_._1).map(x => (x._1, x._2.map(_._2).mkString(",")))

    resultTemp
  }

  /**
    * 将查询条件划分为不同的查询类别，分别进行处理
    *
    * @param query 查询条件
    * @return
    */
  private def parseByType(query: String) = {

    query.substring(0, 2) match {

      case "1:" => Rules.template(query.replaceAll("1:", ""))
      case "2:" => Rules.template(query.replaceAll("2:", ""))
      case "3:" => Rules.template(query.replaceAll("3:", ""))
      case "4:" => Rules.template(query.replaceAll("4:", ""))
      case "5:" => (40003, query.replaceAll("5:", ""))
      case _ => (-1, s"查询条件错误“$query”：条件不存在或存在非法字符")
    }
  }
}