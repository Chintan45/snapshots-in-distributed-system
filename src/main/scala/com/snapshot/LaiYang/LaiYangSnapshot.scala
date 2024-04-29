package com.snapshot.LaiYang

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import com.typesafe.config.{Config, ConfigFactory}
import play.api.libs.json.{Json, Writes}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.io.StdIn.readLine
import scala.util.{Random, Try}

import utils.GraphParser.parseGraph
import utils.MyLogger.{MyLogger, getLogger}
import utils.FileUtils.writeToFile

import scala.io.Source

/**
 * Process Actor that manages internal state and communication with other process actors.
 */
object myProcessActor2 {
  sealed trait Message
  case class BasicMessage(senderName: String, to: String, messageValue: String, isRecorded:Boolean) extends Message
  case class ControlMessage(from: String, to:String, preSnapMsgCount:Int) extends Message
  case class TakeSnapshot(process: String) extends Message
  case class ActorState(processId: String, state:  Map[String, mutable.Set[String]])

  implicit val actorStateWrites: Writes[ActorState] = Json.writes[ActorState]
  private val config: Config = ConfigFactory.load()
  val logger: MyLogger = getLogger(getClass.getName)

  def apply(processId: String, inChannels: Set[(String, String)], outChannels: Set[(String, String)]): Behavior[Message] = {
    Behaviors.setup {
      context =>
        val channelState = inChannels.collect {
          case (from, to) => s"$from$to" -> mutable.Set[String]()
        }.toMap

        // method to save computed state
        def saveState():Unit = {
          val actorState = ActorState(processId, channelState)
          val stateJson = Json.prettyPrint(Json.toJson(actorState))
          val fileName = s"$processId-snapshot.json"
          writeToFile(fileName, stateJson)
        }

        //method to take local snapshot
        def takeSnapshot(process:String = null):Unit = {
          if(!RootProcessor2.recorded(process)) {
            RootProcessor2.recorded(process) = true
            // send Control Message to each outgoing channel of current process
            outChannels.foreach {
              case (from, to) =>
                if(process != to) { // making sure not sending control message again where it came from
                  Thread.sleep(config.getInt("Snapshot.processDelay"))
                  logger.info(s"sending <ControlMessage> $from to $to")
                  val toActor = RootProcessor2.actors(to)
                  val outChannel = s"$from$to"
                  toActor ! ControlMessage(from, to, RootProcessor2.counter(outChannel)+1)
                }
            }
            // take a local snapshot of current process
            saveState()
          }
        }

        // check if process received ControlMessage and Basic Message from all incoming channels
        def hasProcessCaptured: Boolean = {
          var shouldTerminate = true
          inChannels.foreach { case (from, to) =>
            val ch = s"${from}${to}"
            if((channelState(ch).size + 1) != RootProcessor2.counter(ch)) {
              shouldTerminate = false
            }
          }
          shouldTerminate
        }

        Behaviors.receiveMessage {
          // handle basic message sent by other process
          case BasicMessage(from, to, messageValue, recorded) =>
            logger.info(s"Basic message $messageValue, $recorded received @ $to")
            if (recorded) {
              takeSnapshot(to)
            } else {
              val currentChannel = s"$from$to"
              RootProcessor2.counter(currentChannel) = RootProcessor2.counter(currentChannel) - 1 // updating counter
              if (RootProcessor2.recorded(to)) {
                channelState(currentChannel).add(messageValue) // computing channel state
                if (hasProcessCaptured) {
                  saveState()
                  logger.info(s"Process $to received all control + basic Msgs")
                }
              }
            }
            Behaviors.same

          // handle control message sent by other process
          case ControlMessage(from, to, preSnapMsgCount) =>
            logger.info(s"ControlMessage Received @ $processId")
            val ch = s"${from}${to}"
            RootProcessor2.counter(ch) = RootProcessor2.counter(ch) + preSnapMsgCount

            takeSnapshot(to) // taking snapshot

            if (hasProcessCaptured) {
              logger.info(s"Process $to received all control + basic Msgs")
            }
            Behaviors.same

          // handle when process want to initiate a snapshot
          case TakeSnapshot(process) =>
            takeSnapshot(process)
            Behaviors.same

          case _ =>
            Behaviors.unhandled
        }
    }
  }
}

/**
 * The root actor responsible for setting up the system and initiating the snapshot algorithm across all process actors.
 */
object RootProcessor2 {
  sealed trait Message
  case class Setup(nodes: Set[String], edges: Set[(String, String)]) extends Message
  case class BasicMessage(from: String, to: String, messageValue: String) extends Message
  case class Snapshot(processId: String) extends Message
  val actors: mutable.Map[String, ActorRef[myProcessActor2.Message]] = mutable.Map.empty // center Map state to store references of processes
  val counter: mutable.Map[String, Int] = mutable.Map.empty
  val recorded: mutable.Map[String, Boolean] = mutable.Map.empty

  private case object Timeout extends Message  // Internal command for handling timeout
  val logger: MyLogger = getLogger(getClass.getName)
  val config = ConfigFactory.load()

  def apply(): Behavior[Message] = Behaviors.setup {
    context =>
      Behaviors.withTimers {
        timers =>
          var buffer = List.empty[(BasicMessage, Boolean)] // implementing Buffer for non-FIFO channel

          // flushing buffer
          def flushBuffer(): Unit = {
            Random.shuffle(buffer).foreach{
              case (basicMsg, recorded) =>
                Thread.sleep(config.getInt("Snapshot.processDelay"))
                val receivingProcessRef = actors(basicMsg.to)
                receivingProcessRef ! myProcessActor2.BasicMessage(
                  senderName = basicMsg.from,
                  to = basicMsg.to,
                  messageValue = basicMsg.messageValue,
                  isRecorded = recorded
                )
            }
            buffer = List.empty
          }

          // scheduling timeout to flush the buffer
          def scheduleTimeout(): Unit = {
            val config = ConfigFactory.load()
            val bufferTimeOut = config.getInt("Snapshot.LaiYang.bufferTimeOut")
            timers.startSingleTimer("timeout", Timeout, bufferTimeOut.seconds)
          }

          Behaviors.receiveMessage {
            // handle setup input
            case Setup(nodes, edges) =>
              logger.info("Starting system...")
              // creating process actors for each node
              nodes.foreach { node =>
                val inChannels = edges.filter { case (_, dest) => dest == node }
                val outChannels = edges.filter { case (source, _) => source == node }
                val processActorRef = context.spawn(myProcessActor2(node, inChannels, outChannels), s"node_$node")
                actors += (node -> processActorRef)
                recorded += (node -> false)
              }
              edges.foreach {
                case (source, dest) => counter += (s"$source$dest" -> 0)
              }
              logger.info(s"System has been setup with ${actors.size} nodes")
              Behaviors.same

            // handle Basic Message (u --> v)
            case sendMessage: BasicMessage =>
              logger.info(s"sending Message ${sendMessage.messageValue} from ${sendMessage.from} --> ${sendMessage.to}")
              buffer = (sendMessage, recorded(sendMessage.from)) :: buffer
              if (!recorded(sendMessage.from)) {
                val currentChannel = s"${sendMessage.from}${sendMessage.to}"
                counter(currentChannel) = counter(currentChannel) + 1
              }
              if (buffer.size == config.getInt("Snapshot.LaiYang.bufferLimit")) { // flush buffer if reach at limit
                timers.cancel("timeout")
                flushBuffer()
              } else {
                timers.cancel("timeout")
                scheduleTimeout()  // Reset the timeout timer after receiving a message
              }
              Behaviors.same

            // handle snapshot input
            case Snapshot(processId) =>
              logger.info(s"Initiating the Snapshot @ process $processId")
              actors(processId) ! myProcessActor2.TakeSnapshot(processId)
              Behaviors.same

            case Timeout =>
              if (buffer.nonEmpty) {
                flushBuffer()
              }
              Behaviors.same
          }
      }
  }
}

/**
 * Main Object for running Lai Yang Snapshot Algorithm.
 * It initializes the actor system and processes commands to set up the system and initiate snapshots.
 */
object LaiYangSnapshot {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val logger = getLogger(getClass.getName)
    val Processor: ActorSystem[RootProcessor2.Message] = ActorSystem(RootProcessor2(), "MessageProcessorSystem")

    val processDelay = config.getInt("Snapshot.processDelay")
    val graphFilePath = config.getString("Snapshot.Graph.graph2FilePath")
    val transactionsFilePath = config.getString("Snapshot.transactions.transaction2")

    var hasSystemStarted: Boolean = false
    val parsedGraph = parseGraph(graphFilePath)

    val snapshotPattern = """snapshot@(\w+)""".r       // pattern to identify snapshot command
    val messagePattern = """(\w+)\s* --> (\w+)\s* <(.+)>""".r    // pattern to identify basic messages

    logger.info("#######################")
    logger.info("#### Instructions: ####")
    logger.info("#######################")
    logger.info("-> Type 'setup' to setup the system.")
    logger.info("-> Type 'read' to read & execute message transactions from file.")

    logger.info("Start typing below.")


    while(true) {
      val ip = readLine()
      ip.toLowerCase() match {
        // handle set input from command line: setup akka system
        case "setup" =>
          if(!hasSystemStarted) {
            Processor ! RootProcessor2.Setup(parsedGraph.nodes, parsedGraph.edges)
            hasSystemStarted = true
          } else {
            logger.warn("System has already been setup!")
          }
        // handle read input from command line: read transactions
        case "read" =>
          if(!hasSystemStarted) {
            logger.warn("Please setup the system first!")
          } else {
            Try(Source.fromFile(transactionsFilePath)) match {
              case scala.util.Success(file) =>
                val lines = file.getLines()
                lines.foreach(processLine)
                file.close()
              case scala.util.Failure(e) =>
                println(s"Failed to open file: $e")
            }
          }
        case _ =>
          logger.error("Please provide valid input.")
      }
    }

    /**
     * Processes a single line from file, dispatches messages or initiates the snapshot.
     */
    def processLine(line: String): Unit = line match {
      case messagePattern(source, dest, msg) =>
        if(!parsedGraph.edges.contains((source, dest))) {
          logger.error(s"There is no path between $source to $dest")
        } else {
          Thread.sleep(processDelay)
          Processor ! RootProcessor2.BasicMessage(source, dest, msg)
        }
      case snapshotPattern(processId) =>
        if(!parsedGraph.nodes.contains(processId)) {
          logger.error(s"$processId does not exist!")
        } else {
          Processor ! RootProcessor2.Snapshot(processId)
        }
      case _ =>
        logger.error("Invalid input from file")
    }


  }
}
