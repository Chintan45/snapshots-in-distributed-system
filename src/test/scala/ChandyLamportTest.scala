package com.snapshot.ChandyLamport

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import java.nio.file.{Files, Paths}

class ChandyLamportTest extends AnyWordSpecLike with Matchers with BeforeAndAfterAll {
  val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = {
    testKit.shutdownTestKit()
    val testSnapshotPath = Paths.get("snapshots/process1-snapshot.json")
    Files.deleteIfExists(testSnapshotPath)
  }

  "MyProcessActor" should {
    "create a snapshot file when it receives a Marker message" in {
      val probe: TestProbe[myProcessActor.Message] = testKit.createTestProbe[myProcessActor.Message]()
      val processActor: ActorRef[myProcessActor.Message] = testKit.spawn(myProcessActor("process1", Set.empty, Set.empty), "ProcessActor1")

      // Send a Marker message to the actor.
      processActor ! myProcessActor.Marker("process1", "process1")

      Thread.sleep(2000)

      val snapshotPath = Paths.get("snapshots/process1-snapshot.json")
      assert(Files.exists(snapshotPath))
    }
  }
}
