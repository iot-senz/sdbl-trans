package actors

import akka.actor.{Actor, Props}
import components.CassandraTransDbComp
import db.SenzCassandraCluster
import exceptions.EmptySenzException
import org.slf4j.LoggerFactory
import utils.SenzUtils

object SenzReader {

  case class InitReader()

  def props(): Props = Props(new SenzReader())
}

class SenzReader extends Actor {

  import SenzReader._

  def logger = LoggerFactory.getLogger(this.getClass)

  override def preStart() = {
    logger.debug("Start actor: " + context.self.path)
  }

  override def receive: Receive = {
    case InitReader =>
      // listen for user inputs form commandline

      println()
      println()
      println()
      println("Enter only the agent account no here containing 12 numbers. For example  010101110000")
      println("-------------------------------------------------------------------------------------")
      print("Enter the Agent AccountNo - ")

      /*
      println()
      println()
      println("***** Type the SHARE query in the following format after a successful registration of a android client  *****")
      println("***** Else it will not work since the permission is given by this query                                 *****")
      println("***** replace 'agent' with agent account number                                                         *****")
      println("-------------------------------------------------------------------------------------------------------------")
      println("    SHARE #acc #amnt #time #massage #mobile #idno #accounts #bal #balacc #mini #ministat @agent ^sdbltrans   ")
      println("-------------------------------------------------------------------------------------------------------------")
      println()

      */
      /*
      Here
            #amnt [type - RECEIVE] is for the purpose of getting transaction amount from the android client
                          "000000000000" 12 digit max intger string

            #acc  [type - RECEIVE] is for the purpose of getting transaction account from the android client
                          "012345678901" 12 digit account number

            #time [type - RECEIVE] is for the purpose of getting transaction time from the android client
                          "1456626262"  epoc time stamp

            #massage [type - SEND] is for the purpose of sending transaction commit,done from the android client
                          "PUTDONE" or "PUTFAIL"

            #mobile  [type - RECEIVE]  is for the purpose of getting transaction mobile number related to it from agent
                          "0714545455" 10 digit phone number

            #idno    [type - RECEIVE]  is for the purpose of getting id number in order to fetch account numbers from epic
                          "910851462v" id number of length 10

            #accounts [type - SEND] is for the purpose of sending the accounts that was fetched from the id number received
                          "01|012345678910#sansa_test1|1234~02|012345678911|sansa_test2|2345~01|012345678912|sansa_test3|1234"

            #bal   [type - RECEIVE] is for the purpose of getting the balance inquery from the epic host receives a account number
                          "012345678901" max 12 digit account number

            #balacc   [type - SEND]  is for the purpose of sending the balance details
                          "123456" max 12 digit integer. a balance value amoun

            #mini   [type - RECEIVE] is for the purpose of getting the mini statement details from the epic. receives as a account number
                          "013456789201" a 12 digit max account number

            #ministat   [type - SEND] is in the purpose of sending the fetched account numbers from the epic to the agent who requested it
                          141220|123456|C|000000010000~141220|234567|C|000000010000~141220|345678|C|000000010000

      * */



      // read user input from the command line
      val inputUser = scala.io.StdIn.readLine().trim
      val inputSenz = "SHARE #acc #amnt #time #massage #mobile #idno #accounts #bal #balacc #mini #ministat @" + inputUser + " ^sdbltrans"
      logger.debug("Input Senz: " + inputSenz)

      // validate senz
      try {
        SenzUtils.isValidSenz(inputSenz)

        // handle share
        val shareHandlerComp = new ShareHandlerComp with CassandraTransDbComp with SenzCassandraCluster
        context.actorOf(shareHandlerComp.ShareHandler.props(inputSenz))
      } catch {
        case e: EmptySenzException =>
          logger.error("Empty senz")
          println("[ERROR: EMPTY SENZ]")
          self ! InitReader
        case e: Exception =>
          logger.error("Invalid senz", e)
          println("[ERROR: INVALID SENZ]")
          self ! InitReader
      }
  }
}