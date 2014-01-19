package simulator

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import akka.actor.Props
import actor.{PublishingActor, ListeningActor, AppActorSystem}
import scala.concurrent.duration._

object RabbitMQConnection {
  private val connection: Connection = null

  // Return a connection if one doesn't exist. Else create a new one
  def getConnection(): Connection = {
    connection match {
      case null => {
        val factory = new ConnectionFactory()
        factory.setHost("54.193.20.35")
        factory.newConnection()
      }
      case _ => connection
    }
  }

}

object Simulator {

  import AppActorSystem.actorSystem.dispatcher

  def startSimulation = {

    val NUM_OF_LISTENERS = 200
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
      val queueName = channel.queueDeclare("lp.sim." + x, true, true, true, null).getQueue()
      channel.queueBind(queueName, RABBITMQ_EXCHANGE, "")
      val listeningActor = AppActorSystem.actorSystem.actorOf(Props(new ListeningActor(channel, queueName)))
      listeningActor ! "start listening"
    })

    // create an actor that is invoked every 50 ms, i.e. 20 msg/sec
    AppActorSystem.actorSystem.scheduler.schedule(1 second, 50 millis, publishingActor, "")

  }
}
