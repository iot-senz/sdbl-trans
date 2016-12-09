package actors

import java.net.{InetAddress, InetSocketAddress}

import actors.SenzSender.SenzMsg
import akka.actor.{Actor, ActorRef, Props}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.ByteString
import components.TransDbComp
import config.Configuration
import org.slf4j.LoggerFactory
import protocols.Trans
import utils.TransUtils

import scala.concurrent.duration._

case class TransMsg(msgStream: Array[Byte])

case class TransResp(esh: String, status: String, rst: String)

case class TransTimeout()

trait TransHandlerComp {

  this: TransDbComp =>

  object TransHandler {
    def props(trans: Trans): Props = Props(new TransHandler(trans))
  }

  class TransHandler(trans: Trans) extends Actor with Configuration {

    import context._

    def logger = LoggerFactory.getLogger(this.getClass)

    // we need senz sender to send reply back
    val senzSender = context.actorSelection("/user/SenzSender")

    // handle timeout in 5 seconds
    val timeoutCancellable = system.scheduler.scheduleOnce(5 seconds, self, TransTimeout)

    // connect to epic tcp end
    val remoteAddress = new InetSocketAddress(InetAddress.getByName(epicHost), epicPort)
    IO(Tcp) ! Connect(remoteAddress)

    override def preStart() = {
      logger.debug("Start actor: " + context.self.path)
    }

    override def receive: Receive = {
      case c@Connected(remote, local) =>
        logger.debug("TCP connected")

        // transMsg from trans
        val transMsg = TransUtils.getTransMsg(trans)
        val msgStream = new String(transMsg.msgStream)

        logger.debug("Send TransMsg " + msgStream)

        // send TransMsg
        val connection = sender()
        connection ! Register(self)
        connection ! Write(ByteString(transMsg.msgStream))

        // handler response
        context become {
          case CommandFailed(w: Write) =>
            logger.error("CommandFailed[Failed to write]")
          case Received(data) =>
            val response = data.decodeString("UTF-8")
            logger.debug("Received : " + response)

            // cancel timer
            timeoutCancellable.cancel()

            handleResponse(response, connection)
          case "close" =>
            logger.debug("Close")
            connection ! Close
          case _: ConnectionClosed =>
            logger.debug("ConnectionClosed")
            context.stop(self)
          case TransTimeout =>
            // timeout
            logger.error("TransTimeout")
            logger.debug("Resend TransMsg " + msgStream)

            // resend trans
            connection ! Write(ByteString(transMsg.msgStream))
        }
      case CommandFailed(_: Connect) =>
        // failed to connect
        logger.error("CommandFailed[Failed to connect]")
    }

    def handleResponse(response: String, connection: ActorRef) = {
      // parse response and get 'TransResp'
      TransUtils.getTransResp(response) match {
        case TransResp(_, "00", _) =>
          logger.debug("Transaction done")
          transDb.updateTrans(Trans(trans.agent, trans.customer, trans.amount, trans.timestamp, "DONE"))
          val senz = s"DATA #msg PUTDONE @${trans.agent} ^sdbltrans"
          senzSender ! SenzMsg(senz)

        case TransResp(_, status, _) =>
          logger.error("Transaction fail with stats: " + status)
          transDb.updateTrans(Trans(trans.agent, trans.customer, trans.amount, trans.timestamp, "FAIL-" + status))
          val senz = s"DATA #msg PUTFAIL @${trans.agent} ^sdbltrans"
          senzSender ! SenzMsg(senz)

        case transResp =>
          logger.error("Invalid response " + transResp)
          transDb.updateTrans(Trans(trans.agent, trans.customer, trans.amount, trans.timestamp, "FAIL Invalid Response"))
          val senz = s"DATA #msg PUTFAIL @${trans.agent} ^sdbltrans"
          senzSender ! SenzMsg(senz)

      }

      // update db
      // TODO update according to the status
      //transDb.updateTrans(Trans(trans.agent, trans.customer, trans.amount, trans.timestamp, "DONE"))

      // send status back
      // TODO status according to the response
      //val senz = s"DATA #msg PUTDONE @${trans.agent} ^sdbltrans"
      //senzSender ! SenzMsg(senz)

      // disconnect from tcp
      connection ! Close
    }
  }

}
