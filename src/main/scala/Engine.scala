package org.template.clustering

import io.prediction.controller.{Engine,EngineFactory}

case class Query(
  val doc: String,
  val limit: Int,
  val id2: String
) extends Serializable

case class PredictedResult(
  cluster: Double,
  docScores: Array[DocScore]
) extends Serializable {
  override def toString: String = docScores.mkString(",")
}

case class DocScore(
  cluster: Double,
  score: Double,
  id1: String,
  id2: String
) extends Serializable

object TextClusteringEngine extends EngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("tclustering" -> classOf[TextClusteringAlgorithm]),
      	classOf[Serving])
  }
}
