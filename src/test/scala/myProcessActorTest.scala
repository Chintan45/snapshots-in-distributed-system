package com.snapshot.ChandyLamport

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

class myProcessActorTest extends AnyWordSpecLike with Matchers with BeforeAndAfterAll {
  val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "myProcessActor" should {
    "accept basic message" in {
      val probe = testKit.createTestProbe[myProcessActor.Message]()
      val inChannel = Set(("process1", "process2"))
      val outChannel = Set(("process2", "process1"))
      val underTest = testKit.spawn(myProcessActor("process2", inChannel, outChannel))

      underTest ! myProcessActor.BasicMessage("process1", "process2", "message1")

    }
  }
}
