package actors

/**
  * Created by senz on 1/30/17.
  */

import java.net.{InetAddress, InetSocketAddress}

import actors.SenzSender.SenzMsg
import akka.actor.{Actor, ActorRef, Props}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.ByteString

import config.Configuration
import org.slf4j.LoggerFactory
import protocols.AccInq
import utils.AccInquiryUtils

import scala.concurrent.duration._

case class AccInqMsg(msgStream: Array[Byte])

case class AccInqResp(esh: String, resCode: String, authCode: String, accNumbers: String)

case class AccInqTimeout()


trait AccHandlerComp {

  object AccountInquryHandler {
    def props(accInq: AccInq): Props = Props(new AccountInquryHandler(accInq))
  }

  class AccountInquryHandler(accInq: AccInq) extends Actor with Configuration {

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
        val accInqmsg = AccInquiryUtils.getAccInqmsg(accInq)
        val msgStream = new String(accInqmsg.msgStream)

        logger.debug("Send AccInqMsg " + msgStream)

        // send AccInqMsg
        val connection = sender()
        connection ! Register(self)
        connection ! Write(ByteString(accInqmsg.msgStream))

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
            connection ! Write(ByteString(accInqmsg.msgStream))
        }
      case CommandFailed(_: Connect) =>
        // failed to connect
        logger.error("CommandFailed[Failed to connect]")
    }

    def handleResponse(response: String, connection: ActorRef) = {
      val pipeSeparated = getAccountsExtracted(response)

      // parse response and get 'AccInqRes'
      AccInquiryUtils.getAccInqResp(response) match {
        case AccInqResp(_, "00", _, _) =>
          logger.debug("Account Inquiry done")

          val senz = s"DATA #msg PUTDONE #accounts ${pipeSeparated} @${accInq.agent} ^sdbltrans"
          senzSender ! SenzMsg(senz)

        case AccInqResp(_, status, _, _) =>
          logger.error("Account Inquiry fail with stats: " + status)
          val senz = s"DATA #msg PUTFAIL @${accInq.agent} ^sdbltrans"
          senzSender ! SenzMsg(senz)

        case accInqResp =>
          logger.error("Invalid response " + accInqResp)
          val senz = s"DATA #msg PUTFAIL @${accInq.agent} ^sdbltrans"
          senzSender ! SenzMsg(senz)

      }

      // #TODO Handle the response and send it back to the agent with account details
      // disconnect from tcp
      connection ! Close
    }

    def getAccountsExtracted(response: String): String = {
      // get whole string and it might look like this "00000000000"
      // extract the values from it
      // replaces spaces with underscore(_) and hash(#) with pipe(|)
      val spaceWithUnScore = response.substring(98).replace(' ', '_')
      val pipeReplaced = spaceWithUnScore.replace('#', '|')

      logger.debug("Response Accounts" + pipeReplaced)
      pipeReplaced

    }

  }

}