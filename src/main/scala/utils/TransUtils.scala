package utils

import java.text.SimpleDateFormat
import java.util.Calendar

import actors.{TransMsg, TransResp}
import protocols.{Senz, Trans}

object TransUtils {
  def getTrans(senz: Senz): Trans = {
    val agent = senz.sender
    val customer = senz.attributes.getOrElse("acc", "")
    val amnt = senz.attributes.getOrElse("amnt", "").toInt
    val timestamp = senz.attributes.getOrElse("time", "")

    Trans(agent, customer, amnt, timestamp, "PENDING")
  }

  def getTransMsg(trans: Trans) = {
    val fundTranMsg = generateFundTransMsg(trans)
    val esh = generateEsh
    val msg = s"$esh$fundTranMsg"
    val header = generateHeader(msg)

    TransMsg(header ++ msg.getBytes)
  }

  def generateFundTransMsg(trans: Trans) = {
    //val transId = "0000000000000001" // transaction ID, 16 digits // TODO generate unique value
    val pip = "|"   // terminating pip for all attributes
    val rnd = new scala.util.Random               //  genaration of transaction ID
    val randomInt = 100000 + rnd.nextInt(900000) //  random num of 6 digits
    val transId = s"$randomInt$getTransTime"      // random in of length 6 and time stamp of 10 digits

    val payMode = "02" // pay mode
    val epinb = "ffffffffffffffff" // ePINB, 16 digits
    val offset = "ffffffffffff" // offset, 12 digits
    val mobileNo = "0775432015" // customers mobile no
    //val fromAcc = "343434343434" // TODO trans.agent // from account, bank account, 12 digits
    val fromAcc = trans.agent
    //val toAcc = "646464646464" // TODO trans.account // to account, customer account, 12 digits
    val toAcc = trans.customer
    val amnt = "%012d".format(trans.amount) // amount, 12 digits
    //val amnt = trans.amount // amount, 12 digits

   // s"$transId$payMode$epinb$offset$mobileNo$fromAcc$toAcc$amnt"
    s"$transId$pip$payMode$pip$epinb$pip$offset$pip$mobileNo$pip$fromAcc$pip$toAcc$pip$amnt"
  }

  def generateEsh = {
    val pip = "|"      // add a pip after the ESH
    val a = "DEP" // incoming channel mode[mobile]
    val b = "01" // transaction process type[financial]
    val c = "13" // transaction code[Cash deposit{UCSC}]
    val d = "00000002" // TID, 8 digits TODO in prod 00000001
    val e = "000000000000002" // MID, 15 digits TODO in prod 000000000000001
    //val f = "000001" // trace no, 6 digits TODO generate this
    val rnd = new scala.util.Random               // genaration of trace no
    val f = 100000 + rnd.nextInt(900000)     // genaration of trace no
    val g = getTransTime // date time MMDDHHMMSS
    val h = "0001" // application ID, 4 digits
    val i = "0000000000000000" // private data, 16 digits


    //s"$a$b$c$d$e$f$g$h$i"
    s"$a$b$c$d$e$f$g$h$i$pip"
  }

  def generateHeader(msg: String) = {
    val hexLen = f"${Integer.toHexString(msg.getBytes.length).toUpperCase}%4s".replaceAll(" ", "0")

    // convert hex to bytes
    hexLen.replaceAll("[^0-9A-Fa-f]", "").sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)
  }

  def getTransTime = {
    val now = Calendar.getInstance().getTime
    val format = new SimpleDateFormat("MMddhhmmss")

    format.format(now)
  }

  def getTransResp(response: String) = {
    TransResp(response.substring(0, 70), response.substring(77, 79), response.substring(72))
  }

}

//object Main extends App {
//  val agent = "222222222222"
//  val customer = "555555555555"
//  val msg = TransUtils.getTransMsg(Trans(agent, "3423432", customer, "250", "PENDING"))
//  println(msg)
//
//
//  TransUtils.getTransResp(msg.msg) match {
//    case TransResp(_, "11", _) =>
//      println("Transaction done")
//    case TransResp(_, status, _) =>
//      println("hoooo " + status)
//    case transResp =>
//      println("Invalid response " + transResp)
//  }
//}


/*
REQ  DEP011300000002000000000000002936049111010133600010000000000000000|9831111110101336|02|ffffffffffffffff|ffffffffffff|0775432015|111122223333|111111111111|000000411111
RES  DEP0113000000020000000000000029360492016-11-10 15:40:56.174000000000000000000|111015405629|678912

REQ  DEP011300000002000000000000002388662111010200700010000000000000000|7726791110102007|02|ffffffffffffffff|ffffffffffff|0775432015|111122223333|111111111111|000000111111
RES  DEP0113000000020000000000000023886622016-11-10 15:47:27.084000000000000000000|111015472707|678912

REQ  DEP011300000002000000000000002708494111010220500010000000000000000|3354241110102205|02|ffffffffffffffff|ffffffffffff|0775432015|111122223333|222222222222|000000222222
RES  DEP0113000000020000000000000027084942016-11-10 15:49:24.813000000000000000000|111015492433|678912
*/
