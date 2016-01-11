package org.template.clustering

import io.prediction.controller.PDataSource
import io.prediction.controller.EmptyEvaluationInfo
import io.prediction.controller.EmptyActualResult
import io.prediction.controller.Params
import io.prediction.data.store.PEventStore
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.joda.time.{DateTime,LocalDateTime}

import grizzled.slf4j.Logger

case class DataSourceParams(appName: String, groupBy: String) extends Params

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[TrainingData, EmptyEvaluationInfo, Query, EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

  override
  def readTraining(sc: SparkContext): TrainingData = {
    println("Gathering data from event server.")
    val docsRDD: RDD[((String,String),String,DateTime)] = PEventStore.find(
      appName = dsp.appName,
      entityType = Some("doc"),
      eventNames = Some(List("$set")))(sc).map { event =>
        try {
	  ((event.properties.get[String]("id1"), event.properties.get[String]("id2")), event.properties.get[String]("text"),event.eventTime)
        } catch {
          case e: Exception => {
            logger.error(s"Failed to convert event ${event} of. Exception: ${e}.")
            throw e
          }
        }
      }
		
    val now = new org.joda.time.LocalDateTime()
    if (dsp.groupBy=="id1") {
      val docsRDD2 = docsRDD.map(x=>(x._1._1, x._2)).groupByKey.map(x=>(x._1,x._2.mkString(" "))).map(x=>((x._1, "-1"), x._2, now.toDateTime))
      new TrainingData(docsRDD2)
    }
    else if (dsp.groupBy=="id2") {
      val docsRDD2 = docsRDD.map(x=>(x._1._2, x._2)).groupByKey.map(x=>(x._1,x._2.mkString(" "))).map(x=>(("-1", x._1), x._2, now.toDateTime))
      new TrainingData(docsRDD2)
    }
    else {
      new TrainingData(docsRDD)
    }
  }
}

class TrainingData(
  val docs: RDD[((String, String), String, DateTime)]
) extends Serializable
