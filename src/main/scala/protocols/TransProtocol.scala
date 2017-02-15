package protocols

case class Agent(account: String, branch: String)

case class Trans(agent: String, customer: String, amount: Int, timestamp: String, mobile: String, status: String)

case class AccInq(agent: String, idNumber: String)

case class BalInq(agent: String, customer: String)

case class MiniStatInq(agent: String, customer: String)