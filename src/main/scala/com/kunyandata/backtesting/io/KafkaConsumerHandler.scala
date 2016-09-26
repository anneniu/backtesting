package com.kunyandata.backtesting.io

import java.util.Properties

import kafka.consumer.{KafkaStream, ConsumerConnector, Consumer, ConsumerConfig}

/**
  * Created by YangShuai
  * Created on 2016/8/23.
  */
class KafkaConsumerHandler(zookeeper: String, groupId: String, topic: String) {

  private var consumerConnector: ConsumerConnector = null

  /**
    * 获得 KafkaStream 集合
    * @return
    */
  def getStreams: List[KafkaStream[Array[Byte], Array[Byte]]] = {

    val topicCountMap = Map(topic -> 1)
    val consumerMap = consumerConnector.createMessageStreams(topicCountMap)

    consumerMap.get(topic).get
  }

  def createConsumerConfig(zookeeper: String, groupId: String): ConsumerConfig = {

    val props = new Properties()
    props.put("zookeeper.connect", zookeeper)
    props.put("group.id", groupId)
    props.put("auto.offset.reset", "largest")
    props.put("zookeeper.session.timeout.ms", "400")
    props.put("zookeeper.sync.time.ms", "200")
    props.put("auto.commit.interval.ms", "200")

    new ConsumerConfig(props)
  }

  def shutdown(): Unit = consumerConnector.shutdown()

}

object KafkaConsumerHandler {

  def apply(zookeeper: String, groupId: String, topic: String): KafkaConsumerHandler = {

    val handler = new KafkaConsumerHandler(zookeeper, groupId, topic)

    val config = handler.createConsumerConfig(zookeeper, groupId)
    handler.consumerConnector = Consumer.create(config)

    sys.addShutdownHook {
      handler.consumerConnector.shutdown()
    }

    handler
  }

}