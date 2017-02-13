package config

import com.typesafe.config.ConfigFactory

import util.Try

/**
 * Load configurations define in application.conf from here
 *
 * @author eranga herath(erangaeb@gmail.com)
 */
trait Configuration {
  // config object
  val config = ConfigFactory.load()

  // senz config
  lazy val switchName = Try(config.getString("senz.switch-name")).getOrElse("mysensors")
  lazy val clientName = Try(config.getString("senz.client-name")).getOrElse("sdbltrans")

  // server config
  lazy val switchHost = Try(config.getString("switch.host")).getOrElse("localhost")
  lazy val switchPort = Try(config.getInt("switch.port")).getOrElse(9090)

  // epic config
  lazy val epicHost = Try(config.getString("epic.host")).getOrElse("localhost")
  lazy val epicPort = Try(config.getInt("epic.port")).getOrElse(8200)

  // cassandra db config
  lazy val cassandraHost = Try(config.getString("db.cassandra.host")).getOrElse("localhost")
  lazy val cassandraPort = Try(config.getInt("db.cassandra.port")).getOrElse(9160)

  // keys config
  lazy val keysDir = Try(config.getString("keys.dir")).getOrElse(".keys")
  lazy val publicKeyLocation = Try(config.getString("keys.public-key-location")).getOrElse(".keys/id_rsa.pub")
  lazy val privateKeyLocation = Try(config.getString("keys.private-key-location")).getOrElse(".keys/id_rsa")
}
