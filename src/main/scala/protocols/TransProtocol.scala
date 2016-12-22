package protocols

case class Agent(account: String, branch: String)

case class Trans(agent: String, customer: String, amount: Int, timestamp: String, mobile: String, status: String)