package actors

import java.net.{InetAddress, InetSocketAddress}

import actors.SenzSender.SenzMsg
import akka.actor.{Actor, ActorRef, Props}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.ByteString
import config.Configuration
import org.slf4j.LoggerFactory
import protocols.MiniStatInq
import utils.MiniStatementUtils

import scala.concurrent.duration._

/**
  * Created by senz on 2/16/17.
  */


case class MiniStatInqMsg(msgStream: Array[Byte])

case class MiniStatInqResp(esh: String, resCode: String, authCode: String, statementDetails: String)

case class MiniStatInqTimeout()

trait MiniStatementHandlerComp {

  object MiniStatementInquryHandler {
    def props(miniStatInq: MiniStatInq): Props = Props(new MiniStatementInquryHandler(miniStatInq))
  }

  class MiniStatementInquryHandler(miniStatInq: MiniStatInq) extends Actor with Configuration {

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

        // MiniStatInqMsg from trans
        val miniStatInqMsg = MiniStatementUtils.getMiniStatInqMsg(miniStatInq)
        val msgStream = new String(miniStatInqMsg.msgStream)

        logger.debug("Send AccInqMsg " + msgStream)

        // send MiniStatInqMsg
        val connection = sender()
        connection ! Register(self)
        connection ! Write(ByteString(miniStatInqMsg.msgStream))

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
            connection ! Write(ByteString(miniStatInqMsg.msgStream))
        }
      case CommandFailed(_: Connect) =>
        // failed to connect
        logger.error("CommandFailed[Failed to connect]")
    }


    def handleResponse(response: String, connection: ActorRef) = {
      val pipeSeparated = getStatementsExtracted(response)

      // parse response and get 'MinInqResp'
      MiniStatementUtils.getMiniStatInqResp(response) match {
        case MiniStatInqResp(_, "00", _, _) =>
          logger.debug("Account Inquiry done")

          val senz = s"DATA #msg PUTDONE #ministat ${pipeSeparated} @${miniStatInq.agent} ^sdbltrans"
          senzSender ! SenzMsg(senz)

        case MiniStatInqResp(_, status, _, _) =>
          logger.error("Account Inquiry fail with stats: " + status)
          val senz = s"DATA #msg PUTFAIL @${miniStatInq.agent} ^sdbltrans"
          senzSender ! SenzMsg(senz)

        case miniStatInqResp =>
          logger.error("Invalid response " + miniStatInqResp)
          val senz = s"DATA #msg PUTFAIL @${miniStatInq.agent} ^sdbltrans"
          senzSender ! SenzMsg(senz)

      }

    }

    def getStatementsExtracted(response: String): String = {

      /*sample
            03 *** 141220 123456 C 000000010000 *** 141220 234567 C 000000010000  *** 141220 345678 C 000000010000
      * */
      val statementCount = response.substring(100, 102).toInt
      val statesments = response.substring(102)
      var count = 0
      var check = 0

      val builder = StringBuilder.newBuilder

      for (c <- statesments) {
        count = count + 1
        builder.append(c)
        if (count == 6) {
          builder.append("|")
        }
        else if (count == 12) {
          builder.append("|")
        }
        else if (count == 13) {
          builder.append("|")
        }
        else if (count == 25) {
          count = 0
          check = check + 1
          if (check != statementCount)
            builder.append("~")
        }

      }

      val result = builder.toString()
      if (check == statementCount)
        result
      else
        "null"

    }
  }


}