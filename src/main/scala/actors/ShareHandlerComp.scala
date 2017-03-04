package actors

import actors.SenzReader.InitReader
import actors.SenzSender.SenzMsg
import akka.actor.{Actor, Props}
import components.TransDbComp
import org.slf4j.LoggerFactory
import protocols.{Agent, SignatureVerificationFail}
import utils.SenzParser

import scala.concurrent.duration._

case class Share(senzMsg: String)

case class ShareDone()

case class ShareFail()

case class ShareTimeout()

trait ShareHandlerComp {

  this: TransDbComp =>

  object ShareHandler {
    def props(senzMsg: String): Props = Props(new ShareHandler(senzMsg))
  }

  class ShareHandler(senzMsg: String) extends Actor {

    import context._

    def logger = LoggerFactory.getLogger(this.getClass)

    val senzSender = context.actorSelection("/user/SenzSender")
    val senzReader = context.actorSelection("/user/SenzReader")

    // send regSenz in every 4 seconds
    val shareCancellable = system.scheduler.schedule(0 milliseconds, 4 seconds, self, Share(senzMsg))

    // send timeout message after 12 seconds
    val timeoutCancellable = system.scheduler.scheduleOnce(10 seconds, self, ShareTimeout)

    override def preStart() = {
      logger.debug("Start actor: " + context.self.path)
    }

    override def receive: Receive = {
      case Share(senzMsg) =>
        logger.debug("SHARE received: " + senzMsg)

        // TODO only share senz, if senz not already shared with given agent
        // TODO Un share senz
        senzSender ! SenzMsg(senzMsg)
      case ShareDone =>
        // success
        logger.debug("ShareDone")

        println("[OK] SHARE DONE")

        // cancel timers
        shareCancellable.cancel()
        timeoutCancellable.cancel()

        // parse senzMsg
        // create agent in db
        val senz = SenzParser.getSenz(senzMsg)
        transDb.createAgent(Agent(senz.receiver, senz.receiver))

        // reinitialize reader
        senzReader ! InitReader

        // stop the actor
        context.stop(self)
      case ShareFail =>
        // fail
        logger.error("ShareFail")
        println("[ERROR] SHARE FAIL")

        // cancel timers
        shareCancellable.cancel()
        timeoutCancellable.cancel()

        // reinitialize reader
        senzReader ! InitReader

        // stop the actor
        context.stop(self)
      case SignatureVerificationFail =>
        logger.error("Signature verification fail")
        println("[ERROR] SIGNATURE VERIFICATION FAIL")

        // cancel scheduler
        shareCancellable.cancel()
        timeoutCancellable.cancel()

        senzReader ! InitReader

        // stop the actor
        context.stop(self)
      case ShareTimeout =>
        logger.error("Timeout")
        println("[ERROR] SHARE FAIL, TIMEOUT")

        // cancel scheduler
        shareCancellable.cancel()
        timeoutCancellable.cancel()

        // reinitialize reader
        senzReader ! InitReader

        // stop the actor
        context.stop(self)
    }
  }

}
