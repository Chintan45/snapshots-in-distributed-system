package utils

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GraphParserTest extends AnyFlatSpec with Matchers {
  "GraphParser" should "correctly parse graph nodes and edges from a dot file with properties" in {
    val graphString =
      """
        |digraph "Net Graph with 3 nodes" {
        |edge ["class"="link-class"]
        |"0" ["color"="red","label"=<<b>Init</b>>,"fontcolor"="#1020d0"]
        |"0" -> "1" ["weight"="4.0"]
        |"0" -> "2" ["weight"="4.0"]
        |"1" -> "2" ["weight"="2.0"]
        |"2" -> "0" ["weight"="9.0"]
        |}
      """.stripMargin

    val tempFile = java.io.File.createTempFile("graph", ".dot")
    java.nio.file.Files.write(tempFile.toPath, graphString.getBytes)

    val graph = GraphParser.parseGraph(tempFile.getAbsolutePath)
    graph.nodes should contain allOf ("0", "1", "2")
    graph.edges should contain allOf (("0", "1"), ("0", "2"), ("1", "2"), ("2", "0"))

    tempFile.delete()
  }
}
