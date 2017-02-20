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


/*
SUCCESS
Request-massage   SMS010600000002000000000000002787227020310503800011456789456213654994908572057813|02|123456789v
Request-packet    005F534D533031303630303030303030323030303030303030303030303030323738373232373032303331303530333830303031313435363738393435363231333635343939343930383537323035373831337C30327C31323334353637383976
Response-packet   00C4534D53303130363030303030303032303030303030303030303030303032373837323237323031372D30322D30332031303A34353A35372E3330383134353637383934353632313336353430307C3032303331303435353738337C3637383931327C3031233031323334353637383931302373616E736120746573743123313233347E3032233031323334353637383931312373616E736120746573743223323334357E3031233031323334353637383931322373616E73612074657374332331323334
Response-message  SMS0106000000020000000000000027872272017-02-03 10:45:57.308145678945621365400|020310455783|678912|01#012345678910#sansa test1#1234~02#012345678911#sansa test2#2345~01#012345678912#sansa test3#1234
                                                                                                                    AccountType#AccountNumber#OwnerName#CIF ==> 01#012345678910#sansa test1#1234
                                                                                                                                                              > 02#012345678911#sansa test2#2345
                                                                                                                                                              > 01#012345678912#sansa test3#1234


FAIL
Request-massage   SMS0113000000020000000000000029215120203102836000114567894562136545800270600997561|02|123456789v
Request-packet    0060534D53303131333030303030303032303030303030303030303030303032393231353132303230333130323833363030303131343536373839343536323133363534353830303237303630303939373536317C30327C31323334353637383976
Response-packet   0061534D53303131333030303030303032303030303030303030303030303032393231353132323031372D30322D30332031303A32333A35352E3434323134353637383934353632313336353443387C3032303331303233353537307C313032333535
Response-message  SMS0113000000020000000000000029215122017-02-03 10:23:55.4421456789456213654C8|020310235570|102355


*/
