package com.snapshot.ChandyLamport

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import com.typesafe.config.{Config, ConfigFactory}
import play.api.libs.json.{Json, Writes}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.io.StdIn.readLine
import scala.util.Try

import utils.GraphParser.parseGraph
import utils.MyLogger.{MyLogger, getLogger}
import utils.FileUtils.writeToFile

import scala.io.Source

/**
 * Process Actor that manages internal state and communication with other process actors.
 */
object myProcessActor {
  sealed trait Message
  case class BasicMessage(senderName: String, to: String, messageValue: String) extends Message
  case class Marker(from: String, to:String) extends Message
  case class ActorState(processId: String, state:  Map[String, mutable.Queue[String]])

  implicit val actorStateWrites: Writes[ActorState] = Json.writes[ActorState]
  val logger: MyLogger = getLogger(getClass.getName)

  def apply(processId: String, inChannels: Set[(String, String)], outChannels: Set[(String, String)]): Behavior[Message] =
    Behaviors.setup {
      context =>
        var recorded = false
        val marker = mutable.Map[String, Boolean]()
        inChannels.collect {
          case (from, to) => marker += (s"$from$to" -> false)
        }
        val channelState = inChannels.collect {
          case (from, to) => s"$from$to" -> mutable.Queue[String]()
        }.toMap


        // method to take local snapshot of current process
        def takeSnapshot(markerFrom:String = null):Unit = {
          if(!recorded) {
            recorded = true
            // take a local snapshot of current process
            val actorState = ActorState(processId, channelState)
            val stateJson = Json.prettyPrint(Json.toJson(actorState))
            val fileName = s"$processId-snapshot.json"
            writeToFile(fileName, stateJson)
            // send <marker> to each outgoing channel of current process
            outChannels.foreach {
              case (from, to) =>
                if(markerFrom != to) {
                  Thread.sleep(1000)
                  logger.info(s"sending <Marker> $from to $to")
                  val toActor = RootProcessor.actors(to)
                  toActor ! Marker(from, to)
                }
            }
          }
        }

        Behaviors.receiveMessage {
          // handle basic message sent by other process
          case BasicMessage(from, to, messageValue) =>
            recorded = false
            val incomingMessageChannel = s"$from$to"
            channelState(incomingMessageChannel).enqueue(messageValue) // computing channel state
            Behaviors.same

          // handle Marker message sent by other process
          case Marker(from, to) =>
            if(from != to ) { logger.info(s"Marker Received @ $processId") }
            takeSnapshot() // take snapshot
            val incomingMessageChannel = s"$from$to"
            marker(incomingMessageChannel) = true // marking marker received from incoming channel
            // checking if marker received from al incoming channel
            if(marker.values.forall( _ == true)) {
              logger.info(s"processId: $processId received marker from all incoming channels. Local SS has been taken!")
            }
            Behaviors.same

          // handle other case if encountered
          case _ =>
            Behaviors.unhandled
        }
    }
}

/**
 * The Root Actor responsible for setting up the system and initiating the snapshot algorithm across all process actors.
 */
private object RootProcessor {
  sealed trait Message
  case class Setup(nodes: Set[String], edges: Set[(String, String)]) extends Message
  case class BasicMessage(from: String, to: String, messageValue: String) extends Message
  case class Snapshot(processId: String) extends Message
  var actors: Map[String, ActorRef[myProcessActor.Message]] = Map.empty  // center Map state to store references of processes
  private val logger: MyLogger = getLogger(getClass.getName)

  def apply(): Behavior[Message] = Behaviors.setup {
    context => {
      Behaviors.receiveMessage {

        // handle start input
        case Setup(nodes, edges) =>
          logger.info("Starting system...")
          // creating process actors for each node
          nodes.foreach { node =>
            val inChannels = edges.filter { case (_, dest) => dest == node }
            val outChannels = edges.filter { case (source, _) => source == node }
            val processActorRef = context.spawn(myProcessActor(node, inChannels, outChannels), s"node_$node")
            actors += (node -> processActorRef)
          }
          logger.info(s"System has been started with ${actors.size} nodes")
          Behaviors.same


        // handle Basic Message (u --> v)
        case sendMessage: BasicMessage =>
          logger.info(s"sending Message from ${sendMessage.from} --> ${sendMessage.to}")
          val receivingProcessRef = actors(sendMessage.to)
          receivingProcessRef ! myProcessActor.BasicMessage(sendMessage.from, sendMessage.to, sendMessage.messageValue)
          Behaviors.same

        // handle snapshot input
        case Snapshot(processId) =>
          logger.info(s"Initiating the Snapshot @ process $processId")
          actors(processId) ! myProcessActor.Marker(processId, processId)
          Behaviors.same
      }
    }
  }
}

/**
 * Main Object for running Chandy Lamport Snapshot Algorithm.
 * It initializes the actor system and processes commands to set up the system and initiate snapshots.
 */
object ChandyLamportSnapshot {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val logger = getLogger(getClass.getName)
    val Processor: ActorSystem[RootProcessor.Message] = ActorSystem(RootProcessor(), "MessageProcessorSystem1")

    val processDelay = config.getInt("Snapshot.processDelay")
    val graphFilePath = config.getString("Snapshot.Graph.graph1FilePath")
    val transactionsFilePath = config.getString("Snapshot.transactions.transaction1")

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
            Processor ! RootProcessor.Setup(parsedGraph.nodes, parsedGraph.edges)
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
          Processor ! RootProcessor.BasicMessage(source, dest, msg)
        }
      case snapshotPattern(processId) =>
        if(!parsedGraph.nodes.contains(processId)) {
          logger.error(s"$processId does not exist!")
        } else {
          Processor ! RootProcessor.Snapshot(processId)
        }
      case _ =>
        logger.error("Invalid input from file")
    }


  }
}
