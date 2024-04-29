package com.snapshot.LaiYang

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import java.nio.file.{Files, Paths}

class LaiYangTest extends AnyWordSpecLike with Matchers with BeforeAndAfterAll {
  val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = {
    testKit.shutdownTestKit()
    val testSnapshotPath = Paths.get("snapshots/process1-snapshot.json")
    Files.deleteIfExists(testSnapshotPath)
  }

  "MyProcessActor2" should {
    "create a snapshot file when it receives a Snapshot message" in {
      val probe: TestProbe[myProcessActor2.Message] = testKit.createTestProbe[myProcessActor2.Message]()
      val underTest = testKit.spawn(RootProcessor2())
      val processActor: ActorRef[myProcessActor2.Message] = testKit.spawn(myProcessActor2("process1", Set(("process2", "process1")), Set(("process1", "process2"))), "ProcessActor1")

      val nodes = Set("process1", "process2")
      val edges = Set(("process1", "process2"), ("process2", "process1"))
      underTest ! RootProcessor2.Setup(nodes, edges)
      underTest ! RootProcessor2.BasicMessage("process1", "process2", "m")
      Thread.sleep(1000)
      underTest ! RootProcessor2.Snapshot("process1")
      Thread.sleep(5000)

      // Check if the snapshot file has been created.
      val snapshotPath = Paths.get("snapshots/process1-snapshot.json")
      assert(Files.exists(snapshotPath))

    }
  }
}
