package utils


import java.text.SimpleDateFormat
import java.util.Calendar

import actors.{MiniStatInqMsg, MiniStatInqResp}
import protocols.{MiniStatInq, Senz}

/**
  * Created by senz on 2/16/17.
  */
object MiniStatementUtils {

  def getMiniStatInq(senz: Senz): MiniStatInq = {
    val agent = senz.sender
    val customer = senz.attributes.getOrElse("mini", "")

    MiniStatInq(agent, customer)
  }


  def getMiniStatInqMsg(miniStatInq: MiniStatInq) = {
    val fundTranMsg = generateMiniStatInqMessage(miniStatInq)
    val esh = generateEsh
    val msg = s"$esh$fundTranMsg"
    val header = generateHeader(msg)

    MiniStatInqMsg(header ++ msg.getBytes)

  }

  def generateMiniStatInqMessage(miniStatInq: MiniStatInq) = {

    val pip = "|"
    // terminating pip for all attributes
    val customerAcc = miniStatInq.customer
    val rnd = new scala.util.Random
    //  genaration of transaction ID
    val randomInt = 100000 + rnd.nextInt(900000)
    //  random num of 6 digits
    val transId = s"$randomInt$getTransTime" // random in of length 6 and time stamp of 10 digits

    val requestMode = "02" // pay mode


    s"$transId$pip$requestMode$pip$customerAcc"

  }

  def generateEsh() = {
    val pip = "|"
    // add a pip after the ESH
    val a = "SMS"
    // incoming channel mode[mobile]
    val b = "02"
    // transaction process type[financial]
    val c = "07"
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

  def getMiniStatInqResp(response: String) = {
    MiniStatInqResp(response.substring(0, 70), response.substring(77, 79), response.substring(72, 80), response.substring(82))
    //Should be like AccInqResp(esh: String, resCode: String, authCode: String, accNumbers: String)
    //                          0-70              77-79

  }


}


/*

Request-message   SMS0107000000020000000000000023678130216090906000114567894562136542090810831155756|02|123456789123|3C9770FCC9D47189|000000000000|94771137156
Request-packet    008C534D53303130373030303030303032303030303030303030303030303032333637383133303231363039303930363030303131343536373839343536323133363534323039303831303833313135353735367C30327C3132333435363738393132337C334339373730464343394434373138397C3030303030303030303030307C3934373731313337313536

Response-packet   00AF534D53303130373030303030303032303030303030303030303030303032333637383133323031372D30322D31362030393A30343A31332E3039383134353637383934353632313336353430307C3032313630393034313339377C3637383931327C3033313431323230313233343536433030303030303031303030303134313232303233343536374330303030303030313030303031343132323033343536373843303030303030303130303030
Response-message  SMS0107000000020000000000000023678132017-02-16 09:04:13.098145678945621365400|021609041397|678912|03141220123456C000000010000141220234567C000000010000141220345678C000000010000

                    03 *** 141220 123456 C 000000010000 *** 141220 234567 C 000000010000  *** 141220 345678 C 000000010000
                    no of entries
                            yymmdd traceno C/D amount
//Read the document corresponding to MiniStatement

* */