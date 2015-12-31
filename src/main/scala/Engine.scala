package org.template.clustering

import io.prediction.controller.{Engine,EngineFactory}

case class Query(
  val doc: String,
  val limit: Int
) extends Serializable

case class PredictedResult(
  clusterNo: Double,
  docScores: Array[DocScore]
) extends Serializable {
  override def toString: String = docScores.mkString(",")
}

case class DocScore(
  cluster: Double,
  score: Double,
  id: String
) extends Serializable

object TextSimilarityEngine extends EngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("tclustering" -> classOf[TextClusteringAlgorithm]),
      	classOf[Serving])
  }
}
