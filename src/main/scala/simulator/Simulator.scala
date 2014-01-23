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
  def getConnection(host: String): Connection = {
    connection match {
      case null => {
        val factory = new ConnectionFactory()
        factory.setHost(host)
        factory.newConnection()
      }
      case _ => connection
    }
  }

}

// Dummy message loaded from a file
object DummyMessage {
  val byteArray: Array[Byte] = loadFromFile("DummyMessage")

  def loadFromFile(fileName: String): Array[Byte] = {
    val source: BufferedSource = scala.io.Source.fromFile(fileName)(scala.io.Codec.ISO8859)
    val byteArray = source.map(_.toByte).toArray
    source.close()
    byteArray
  }
}

// The message to be send to RabbitMQ
case class SimulatedMessage(timestamp: Long, payload: Array[Byte])

// The simulation code
object Simulator {

  import AppActorSystem.actorSystem.dispatcher

  def startConsumers(host: String, exchange: String, numOfConsumers: Int) = {

    // create the connection
    val connection = RabbitMQConnection.getConnection(host)

    // create a new sending channel on which we declare the exchange
    val sendingChannel = connection.createChannel()
    sendingChannel.exchangeDeclare(exchange, "fanout")

    // setup NUM_OF_LISTENERS listeners
    (1 to numOfConsumers) foreach (x => {

      val channel = connection.createChannel()
      val queueName = channel.queueDeclare("sim." + System.currentTimeMillis(), false, true, true, null).getQueue()
      channel.queueBind(queueName, exchange, "")

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

    println(numOfConsumers + " consumers started.")

  }

  def startPublishing(host: String, exchange: String, interval: Int) = {
    // create the connection
    val connection = RabbitMQConnection.getConnection(host)

    // create a new sending channel on which we declare the exchange
    val sendingChannel = connection.createChannel()
    val publishingActor = AppActorSystem.actorSystem.actorOf(Props(new PublishingActor(channel = sendingChannel, exchange = exchange)))

    // schedule to invoke an actor every interval milli seconds
    AppActorSystem.actorSystem.scheduler.schedule(50 millis, interval millis, publishingActor, "")
  }
}
