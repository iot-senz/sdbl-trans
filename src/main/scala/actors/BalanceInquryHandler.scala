package actors

/**
  * Created by senz on 2/13/17.
  */

import java.net.{InetAddress, InetSocketAddress}

import actors.SenzSender.SenzMsg
import akka.actor.{Actor, ActorRef, Props}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.ByteString
import config.Configuration
import org.slf4j.LoggerFactory
import protocols.BalInq
import utils.BalanceUtils

import scala.concurrent.duration._

case class BalInqMsg(msgStream: Array[Byte])

case class BalInqResp(esh: String, resCode: String, authCode: String, balance: String)

case class BalInqTimeout()

trait BalInqHandlerComp {

  object BalanceInquryHandler {
    def props(balInq: BalInq): Props = Props(new BalanceInquryHandler(balInq))
  }

  class BalanceInquryHandler(balInq: BalInq) extends Actor with Configuration {

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
        val balInqMsg = BalanceUtils.getBalInqMsg(balInq)
        val msgStream = new String(balInqMsg.msgStream)

        logger.debug("Send BalInqMsg " + msgStream)

        // send AccInqMsg
        val connection = sender()
        connection ! Register(self)
        connection ! Write(ByteString(balInqMsg.msgStream))

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
            logger.error("AccInqTimeout")
            logger.debug("Resend AccInqMsg " + msgStream)

            // resend trans
            connection ! Write(ByteString(balInqMsg.msgStream))
        }
      case CommandFailed(_: Connect) =>
        // failed to connect
        logger.error("CommandFailed[Failed to connect]")
    }

    def handleResponse(response: String, connection: ActorRef) = {
      val balance = getBalanceExtracted(response)

      // parse response and get 'AccInqRes'
      BalanceUtils.getBalInqResp(response) match {
        case BalInqResp(_, "00", _, _) =>
          logger.debug("Account Inquiry done")

          val senz = s"DATA #msg PUTDONE #bal ${balance} @${balInq.agent} ^sdbltrans"
          senzSender ! SenzMsg(senz)

        case BalInqResp(_, status, _, _) =>
          logger.error("Account Inquiry fail with stats: " + status)
          val senz = s"DATA #msg PUTFAIL @${balInq.agent} ^sdbltrans"
          senzSender ! SenzMsg(senz)

        case balInqResp =>
          logger.error("Invalid response " + balInqResp)
          val senz = s"DATA #msg PUTFAIL @${balInq.agent} ^sdbltrans"
          senzSender ! SenzMsg(senz)

      }

      // disconnect from tcp
      connection ! Close
    }

    def getBalanceExtracted(response: String): String = {

      val balance = response.substring(88, 100)
      balance

    }


  }

}

