package simulator


import com.rabbitmq.client._
import akka.actor.Props
import actor.{PublishingActor, AppActorSystem}
import scala.io.BufferedSource
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import scala.concurrent.duration._

object JacksonMapper {
  val mapper = (new ObjectMapper()).registerModule(DefaultScalaModule)
}

object RabbitMQConnection {
  private val connection: Connection = null

  // Return a connection if one doesn't exist. Else create a new one
  def getConnection(): Connection = {
    connection match {
      case null => {
        val factory = new ConnectionFactory()
        factory.setHost("107.22.8.250")
        factory.newConnection()
      }
      case _ => connection
    }
  }

}

// Dummy message loaded from a file
object DummyMessage {
  val byteArray: Array[Byte] = loadFromFile("./src/main/resources/DummyMessage")

  def loadFromFile(fileName: String): Array[Byte] = {
    val source: BufferedSource = scala.io.Source.fromFile(fileName)(scala.io.Codec.ISO8859)
    val byteArray = source.map(_.toByte).toArray
    source.close()
    byteArray
  }
}

case class SimulatedMessage(timestamp: Long, payload: Array[Byte])


object Simulator {

  import AppActorSystem.actorSystem.dispatcher

  def startSimulation = {

    val NUM_OF_LISTENERS = 20
    val RABBITMQ_EXCHANGE = "lp.sim.exchange"

    // create the connection
    val connection = RabbitMQConnection.getConnection()

    // create a new sending channel on which we declare the exchange
    val sendingChannel = connection.createChannel()
    sendingChannel.exchangeDeclare(RABBITMQ_EXCHANGE, "fanout")

    val publishingActor = AppActorSystem.actorSystem.actorOf(Props(new PublishingActor(channel = sendingChannel, exchange = RABBITMQ_EXCHANGE)))

    // setup NUM_OF_LISTENERS listeners
    (1 to NUM_OF_LISTENERS) foreach (x => {

      val channel = connection.createChannel()
      val queueName = channel.queueDeclare("lp.sim." + x, false, true, true, null).getQueue()
      channel.queueBind(queueName, RABBITMQ_EXCHANGE, "")

      val msgConsumer = new DefaultConsumer(channel) {

        override def handleDelivery(consumerTag: String,
           envelope: Envelope,
           properties:AMQP.BasicProperties,
           body: Array[Byte])
          {
            val receivedTime = System.currentTimeMillis()
            val msg = new String(body)
            val simulatedMessage = JacksonMapper.mapper.readValue(msg, classOf[SimulatedMessage])
            val latency = receivedTime - simulatedMessage.timestamp

            println("Message " + simulatedMessage.timestamp + " received with latency " + latency + " ms.")
          }
      }

      channel.basicConsume(queueName, true, queueName, msgConsumer)
    })

    // schedule to invoke an actor every 50 ms, i.e. 20 msg/sec
    AppActorSystem.actorSystem.scheduler.schedule(50 millis, 50 millis, publishingActor, "")

    //publishingActor ! "start publishing"

  }



  /*
(1 to NUM_OF_LISTENERS) foreach (x => {
  val channel = connection.createChannel()
  val queueName = channel.queueDeclare("lp.sim." + x, false, true, true, null).getQueue()
  channel.queueBind(queueName, RABBITMQ_EXCHANGE, "")
  val listeningActor = AppActorSystem.actorSystem.actorOf(Props(new ListeningActor(channel, queueName)))
  listeningActor ! "start listening"
})
*/
}
