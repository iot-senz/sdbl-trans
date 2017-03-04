package utils

import java.text.SimpleDateFormat
import java.util.Calendar

import actors.{BalInqMsg, BalInqResp}
import protocols.{BalInq, Senz}

/**
  * Created by senz on 2/13/17.
  */
object BalanceUtils {
  def getBalInq(senz: Senz): BalInq = {
    val agent = senz.sender
    val customer = senz.attributes.getOrElse("balacc", "")

    BalInq(agent, customer)
  }

  def getBalInqMsg(balInq: BalInq) = {
    val fundTranMsg = generateBalInqMsg(balInq)
    val esh = generateEsh
    val msg = s"$esh$fundTranMsg"
    val header = generateHeader(msg)

    BalInqMsg(header ++ msg.getBytes)

  }

  def generateBalInqMsg(balInq: BalInq) = {

    val pip = "|"
    // terminating pip for all attributes
    val rnd = new scala.util.Random
    //  genaration of transaction ID
    val randomInt = 100000 + rnd.nextInt(900000)
    //  random num of 6 digits
    val transId = s"$randomInt$getBalInqTime" // random in of length 6 and time stamp of 10 digits

    val payMode = "02"
    // pay mode
    val ePINB = "ffffffffffffffff"
    // ePINB, 16 digits
    val offset = "000000000000" // offset, 12 digits

    val balAcc = balInq.customer
    val mobileNo = "0123456789" // a hard coded value for the mobile

    s"$transId$pip$payMode$pip$balAcc$pip$ePINB$pip$offset$pip$mobileNo"
  }

  def generateEsh = {
    val pip = "|"
    // add a pip after the ESH
    val a = "DEP"
    // incoming channel mode[mobile]
    val b = "01"
    // transaction process type[financial]
    val c = "03"
    // transaction code[Cash deposit{UCSC}]
    val d = "00000001"
    // TID, 8 digits
    val e = "000000000000001"
    // MID, 15 digits
    val rnd = new scala.util.Random
    // genaration of trace no
    val f = 100000 + rnd.nextInt(900000)
    // genaration of trace no
    val g = getBalInqTime
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

  def getBalInqTime = {
    val now = Calendar.getInstance().getTime
    val format = new SimpleDateFormat("MMddhhmmss")

    format.format(now)
  }

  def getBalInqResp(response: String) = {
    BalInqResp(response.substring(0, 70), response.substring(77, 79), response.substring(72, 76), response.substring(80))
    //              ESH                       rescoed                   authcode                balance

  }


}


/*

Request-message  SMS0103000000020000000000000025928140213164840000114567894562136544175375128073668|02|123456789123|3C9770FCC9D47189|000000000000|94771137156
Request-packet   008C534D53303130333030303030303032303030303030303030303030303032353932383134303231333136343834303030303131343536373839343536323133363534343137353337353132383037333636387C30327C3132333435363738393132337C334339373730464343394434373138397C3030303030303030303030307C3934373731313337313536
Response-packet  008A534D53303130333030303030303032303030303030303030303030303032353932383134323031372D30322D31332031363A34333A34352E3130343134353637383934353632313336353430307C3032313331363433343530317C3637383931327C30313031303030433030303030303030313030303031303130303043303030303030303031303030
Response-message SMS0103000000020000000000000025928142017-02-13 16:43:45.104145678945621365400|021316434501|678912|0101000C0000000010000101000C000000001000

      balance            01 01 000 C 000000001000      01 01 000 C 000000001000
Response Code 00




* */
