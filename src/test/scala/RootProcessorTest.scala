package com.snapshot.ChandyLamport

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

class RootProcessorTest extends AnyWordSpecLike with Matchers with BeforeAndAfterAll {
  val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "RootProcessor" should {
    "accept setup system" in {
      val probe = testKit.createTestProbe[RootProcessor.Message]()
      val underTest = testKit.spawn(RootProcessor())

      val nodes = Set("a", "b")
      val edges = Set(("a", "b"), ("b", "a"))
      underTest ! RootProcessor.Setup(nodes, edges)

    }
  }
}
