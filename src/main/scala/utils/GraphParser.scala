package utils

import scala.io.Source

object GraphParser {
  case class Graph(nodes: Set[String], edges: Set[(String, String)])

  def parseGraph(filePath: String): Graph = {
    var nodes = Set[String]()
    var edges = Set[(String, String)]()

    val source = Source.fromFile(filePath)
    try {
      for (line <- source.getLines()) {
        if (line.matches("\"\\w+\" -> \"\\w+\".*")) {
          val Array(src, dst) = line.split(" -> ")
          nodes += src.substring(1, src.length - 1)
          nodes += dst.substring(1, dst.length - 18)
          edges += ((src.substring(1, src.length - 1), dst.substring(1, dst.length - 18)))
        }
      }
    } finally {
      source.close()
    }
    Graph(nodes, edges)
  }

  def main(args: Array[String]): Unit = {
    val filePath = "src/resources/graph.ngs.dot"
    val graph = parseGraph(filePath)
    println("Nodes: " + graph.nodes.mkString(", "))
    println("Edges: " + graph.edges.mkString(", "))
  }
}
