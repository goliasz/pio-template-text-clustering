package org.template.clustering

import io.prediction.controller.PPreparator
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.joda.time.DateTime

class PreparedData(
  val docs: RDD[((String,String),String,DateTime)]
) extends Serializable

class Preparator
  extends PPreparator[TrainingData, PreparedData] {

  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    new PreparedData(docs = trainingData.docs)
  }
}


