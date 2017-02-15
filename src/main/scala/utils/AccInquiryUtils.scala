package utils

import java.text.SimpleDateFormat
import java.util.Calendar

import actors.{AccInqMsg, AccInqResp}
import protocols.{Senz, AccInq}

/**
  * Created by senz on 1/30/17.
  */
object AccInquiryUtils {

  def getIdNumber(senz: Senz): AccInq = {
    val idNumber = senz.attributes.getOrElse("idno", "")
    val agent = senz.sender
    AccInq(agent, idNumber)
  }

  def getAccInqmsg(accInq: AccInq) = {

    val fundTranMsg = generateAccInqMassage(accInq)
    val esh = generateEsh
    val msg = s"$esh$fundTranMsg"
    val header = generateHeader(msg)

    AccInqMsg(header ++ msg.getBytes)
  }

  def generateAccInqMassage(accInq: AccInq) = {

    val pip = "|"
    // terminating pip for all attributes
    val idNumber = accInq.idNumber
    val rnd = new scala.util.Random
    //  genaration of transaction ID
    val randomInt = 100000 + rnd.nextInt(900000)
    //  random num of 6 digits
    val transId = s"$randomInt$getTransTime" // random in of length 6 and time stamp of 10 digits

    val requestMode = "02" // pay mode


    s"$transId$pip$requestMode$pip$idNumber"
  }

  def generateEsh() = {
    val pip = "|"
    // add a pip after the ESH
    val a = "SMS"
    // incoming channel mode[mobile]
    val b = "01"
    // transaction process type[financial]
    val c = "06"
    // transaction code[Cash deposit{UCSC}]
    val d = "00000002"
    // TID, 8 digits
    val e = "000000000000002"
    // MID, 15 digits
    val rnd = new scala.util.Random
    // generation of trace no
    val f = 100000 + rnd.nextInt(900000)
    // generation of trace no
    val g = getTransTime
    // date time MMDDHHMMSS
    val h = "0001"
    // application ID, 4 digits
    val i = "0000000000000000" // private data, 16 digits


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

  def getAccInqResp(response: String) = {
    AccInqResp(response.substring(0, 70), response.substring(77, 79), response.substring(72, 80), response.substring(82))
    //Should be like AccInqResp(esh: String, resCode: String, authCode: String, accNumbers: String)
    //                          0-70              77-79
    // TODO
  }

}
