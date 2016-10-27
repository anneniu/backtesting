package com.kunyandata.backtesting.config

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

import org.w3c.dom.Element

import scala.collection.mutable

/**
  * Created by yangshuai on 2016/2/26.
  */
object Configuration {

  def getConfigurations(path:String): (mutable.Map[String, String], mutable.Map[String, String]) = {

    val redis = mutable.Map[String, String]()
    val kafka = mutable.Map[String, String]()

    val file = new File(path)

    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)

    //redis
    val redisRoot = doc.getElementsByTagName("redis").item(0).asInstanceOf[Element]
    val redisIp = redisRoot.getElementsByTagName("ip").item(0).getTextContent
    val redisPort = redisRoot.getElementsByTagName("port").item(0).getTextContent
    val redisDb = redisRoot.getElementsByTagName("db").item(0).getTextContent
    val redisAuth = redisRoot.getElementsByTagName("auth").item(0).getTextContent

    redis.put("ip", redisIp)
    redis.put("port", redisPort)
    redis.put("db", redisDb)
    redis.put("auth", redisAuth)

    //kafka
    val kafkaRoot = doc.getElementsByTagName("kafka").item(0).asInstanceOf[Element]
    val zookeeper = kafkaRoot.getElementsByTagName("zookeeper").item(0).getTextContent
    val groupId = kafkaRoot.getElementsByTagName("groupId").item(0).getTextContent
    val brokerList = kafkaRoot.getElementsByTagName("brokerList").item(0).getTextContent
    val receiveTopic = kafkaRoot.getElementsByTagName("receiveTopic").item(0).getTextContent
    val sendTopic = kafkaRoot.getElementsByTagName("sendTopic").item(0).getTextContent

    kafka.put("zookeeper", zookeeper)
    kafka.put("groupId", groupId)
    kafka.put("brokerList", brokerList)
    kafka.put("receiveTopic", receiveTopic)
    kafka.put("sendTopic", sendTopic)

    (redis, kafka)
  }

}
