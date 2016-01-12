package org.template.clustering

import io.prediction.controller.P2LAlgorithm
import io.prediction.controller.Params
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.feature.{Word2Vec,Word2VecModel}
import org.apache.spark.mllib.clustering.{KMeans, KMeansModel}
import grizzled.slf4j.Logger
import org.apache.spark.mllib.feature.Normalizer
import org.apache.spark.mllib.linalg.DenseVector
import org.joda.time.DateTime

case class AlgorithmParams(
  val seed: Int,
  val minCount: Int,
  val learningRate: Double,
  val numIterations: Int,
  val vectorSize: Int,
  val kmNumClusters: Int,
  val kmNumIterations: Int,
  val kmRuns: Int,
  val kmInitMode: String,
  val kmSeed: Int
) extends Params

class TCModel(
  val word2VecModel: Word2VecModel,
  val kMeansModel: KMeansModel,
  val docPairs: List[((String,String,DateTime), breeze.linalg.DenseVector[Double], Int)],
  val vectorSize: Int
) extends Serializable {}

class TextClusteringAlgorithm(val ap: AlgorithmParams) extends P2LAlgorithm[PreparedData, TCModel, Query, PredictedResult] {

  @transient lazy val logger = Logger[this.type]

  def train(sc: SparkContext, data: PreparedData): TCModel = {
    println("Training text clustering model.")

    val art1 = data.docs.map(x=>(x._2.toLowerCase.replace(".","").split(" ").filter(k => !stopwords.contains(k)).map(normalizet).filter(_.trim.length>2).toSeq, (x._1._1, x._1._2, x._3) ))
    
    val word2vec = new Word2Vec()
    word2vec.setSeed(ap.seed)
    word2vec.setMinCount(ap.minCount)
    word2vec.setLearningRate(ap.learningRate)
    word2vec.setNumIterations(ap.numIterations)
    word2vec.setVectorSize(ap.vectorSize)	
	
    val model = word2vec.fit(art1.map(_._1).cache)

    val art_pairs = art1.map(x => (x._2, new DenseVector(divArray(x._1.map(m => wordToVector(m, model, ap.vectorSize).toArray).reduceLeft(sumArray),x._1.length)).asInstanceOf[Vector]))	

    var kmModel = KMeans.train(art_pairs.map(_._2).cache, ap.kmNumClusters, ap.kmNumIterations, ap.kmRuns, ap.kmInitMode, ap.kmSeed)

    val normalizer1 = new Normalizer()
    val art_pairsb = art_pairs.map(x=>(x._1, normalizer1.transform(x._2), x._2)).map(x=>(x._1,{new breeze.linalg.DenseVector(x._2.toArray)}, kmModel.predict(x._3)))	

    new TCModel(model, kmModel, art_pairsb.collect.toList, ap.vectorSize)
  }

  def predict(model: TCModel, query: Query): PredictedResult = {
    //Prepare query vector
    val td02 = query.doc.split(" ").filter(k => !stopwords.contains(k)).map(normalizet).filter(_.trim.length>2).toSeq
    val td02w2v = new DenseVector(divArray(td02.map(m => wordToVector(m, model.word2VecModel, model.vectorSize).toArray).reduceLeft(sumArray),td02.length)).asInstanceOf[Vector]
    val normalizer1 = new Normalizer()
    val td02w2vn = normalizer1.transform(td02w2v)
    val td02bv = new breeze.linalg.DenseVector(td02w2vn.toArray)
        
    val qryClusterNo = model.kMeansModel.predict(td02w2v)

    val r = model.docPairs.filter(x=>{if(!query.matchClusters) true else qryClusterNo==x._3}).filter(x=>{if(query.id1.trim.isEmpty) true else query.id1==x._1._1}).filter(x=>{if(query.id2.trim.isEmpty) true else query.id2==x._1._2}).map(x=>(td02bv.dot(x._2),x._1,x._3)).sortWith(_._1>_._1).take(query.limit).map(x=>{new DocScore(x._3, x._1, x._2._1, x._2._2, x._2._3)})

    PredictedResult(cluster = qryClusterNo, docScores = r.toArray)
  }

  def sumArray (m: Array[Double], n: Array[Double]): Array[Double] = {
    for (i <- 0 until m.length) {m(i) += n(i)}
    return m
  }

  def divArray (m: Array[Double], divisor: Double) : Array[Double] = {
    for (i <- 0 until m.length) {m(i) /= divisor}
    return m
  }

  def wordToVector (w:String, m: Word2VecModel, s: Int): Vector = {
    try {
      return m.transform(w)
    } catch {
      case e: Exception => return Vectors.zeros(s)
    }  
  }

  def normalizet(line: String) = java.text.Normalizer.normalize(line,java.text.Normalizer.Form.NFKD).replaceAll("\\p{InCombiningDiacriticalMarks}+","").toLowerCase

  val regex = """[^0-9]*""".r

  val stopwords = Array("a", "about", "above", "above", "across", "after", "afterwards", "again", "against", "all", "almost", "alone", "along", "already", "also","although","always","am","among", "amongst", "amoungst", "amount",  "an", "and", "another", "any","anyhow","anyone","anything","anyway", "anywhere", "are", "around", "as",  "at", "back","be","became", "because","become","becomes", "becoming", "been", "before", "beforehand", "behind", "being", "below", "beside", "besides", "between", "beyond", "bill", "both", "bottom","but", "by", "call", "can", "cannot", "cant", "co", "con", "could", "couldnt", "cry", "de", "describe", "detail", "do", "done", "down", "due", "during", "each", "eg", "eight", "either", "eleven","else", "elsewhere", "empty", "enough", "etc", "even", "ever", "every", "everyone", "everything", "everywhere", "except", "few", "fifteen", "fify", "fill", "find", "fire", "first", "five", "for", "former", "formerly", "forty", "found", "four", "from", "front", "full", "further", "get", "give", "go", "had", "has", "hasnt", "have", "he", "hence", "her", "here", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "him", "himself", "his", "how", "however", "hundred", "ie", "if", "in", "inc", "indeed", "interest", "into", "is", "it", "its", "itself", "keep", "last", "latter", "latterly", "least", "less", "ltd", "made", "many", "may", "me", "meanwhile", "might", "mill", "mine", "more", "moreover", "most", "mostly", "move", "much", "must", "my", "myself", "name", "namely", "neither", "never", "nevertheless", "next", "nine", "no", "nobody", "none", "noone", "nor", "not", "nothing", "now", "nowhere", "of", "off", "often", "on", "once", "one", "only", "onto", "or", "other", "others", "otherwise", "our", "ours", "ourselves", "out", "over", "own","part", "per", "perhaps", "please", "put", "rather", "re", "same", "see", "seem", "seemed", "seeming", "seems", "serious", "several", "she", "should", "show", "side", "since", "sincere", "six", "sixty", "so", "some", "somehow", "someone", "something", "sometime", "sometimes", "somewhere", "still", "such", "system", "take", "ten", "than", "that", "the", "their", "them", "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein", "thereupon", "these", "they", "thickv", "thin", "third", "this", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "top", "toward", "towards", "twelve", "twenty", "two", "un", "under", "until", "up", "upon", "us", "very", "via", "was", "we", "well", "were", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "whose", "why", "will", "with", "within", "without", "would", "yet", "you", "your", "yours", "yourself", "yourselves", "the").toSet

}
