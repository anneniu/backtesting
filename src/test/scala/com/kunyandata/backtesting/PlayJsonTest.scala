package com.kunyandata.backtesting

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json._

import scala.collection.mutable

/**
  * Created by YangShuai
  * Created on 2016/8/31.
  */
class PlayJsonTest extends FlatSpec with Matchers {

  it should "parse json" in {

    val json = "{\n    \"uid\": 10002,\n    \"session\": \"3232332323\",\n    \"condition\": \"1:wr值小于25+2:主力抢筹的房地产开发股+3:恒大集团\",\n    \"start_time\": \"2016-07-21\",\n    \"end_time\": \"2016-08-22\"\n}"
    val result = Json.parse(json)

    val uid = (result \ "uid").as[Long]
    val session = (result \ "session").as[String]
    val condition = (result \ "condition").as[String]
    val startTime = (result \ "start_time").as[String]
    val endTime = (result \ "end_time").as[String]

    uid should be (10002l)
    session should be ("3232332323")
    condition should be ("1:wr值小于25+2:主力抢筹的房地产开发股+3:恒大集团")
    startTime should be ("2016-07-21")
    endTime should be ("2016-08-22")
  }

  it should "generate json string" in {

    val list = mutable.ListBuffer[String]("a", "b", "c")
    val jsList = Json.toJson(list)

    val jsValue = Json.obj("1" -> "a", "list" -> list)
    println(Json.stringify(jsValue))

    println(Long.MaxValue)
  }

}
