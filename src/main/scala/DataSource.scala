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
import org.joda.time.DateTime

import grizzled.slf4j.Logger

case class DataSourceParams(appName: String) extends Params

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[TrainingData, EmptyEvaluationInfo, Query, EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

/*
  override
  def readTraining(sc: SparkContext): TrainingData = {
    println("Gathering data from event server.")
    val docsRDD: RDD[((String,String),String)] = PEventStore.aggregateProperties(
      appName = dsp.appName,
      entityType = "doc",
      required = Some(List("id1","id2","text")))(sc).map { case (entityId, properties) =>
        try {
	  ((properties.get[String]("id1"), properties.get[String]("id2")), properties.get[String]("text"))
        } catch {
          case e: Exception => {
            logger.error(s"Failed to get properties ${properties} of" +
              s" ${entityId}. Exception: ${e}.")
            throw e
          }
        }
      }
		
    new TrainingData(docsRDD)
  }
*/

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
		
    new TrainingData(docsRDD)
  }
}

class TrainingData(
  val docs: RDD[((String, String), String, DateTime)]
) extends Serializable
