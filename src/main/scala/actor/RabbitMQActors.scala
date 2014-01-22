package actor

import akka.actor.{Props, ActorSystem, Actor}
import com.rabbitmq.client.{QueueingConsumer, Channel}
import simulator.{JacksonMapper, DummyMessage, SimulatedMessage}
import java.io.StringWriter

object AppActorSystem {
  // Create the 'mq-simulation' actor system
  val actorSystem = ActorSystem("mq-simulation")
}

class SendingActor(channel: Channel, queue: String) extends Actor {

  def receive = {
    case some: String => {
      val msg = (some + " : " + System.currentTimeMillis())
      channel.basicPublish("", queue, null, msg.getBytes())
      println(msg)
    }
    case _ => {}
  }
}

class PublishingActor(channel: Channel, exchange: String) extends Actor {

  /**
   * When we receive a message we sent it using the configured channel
   */
  def receive = {
    case some: String => {

      val out = new StringWriter

      //val msg: SimulatedMessage = SimulatedMessage(System.currentTimeMillis(), "This is a short message".getBytes())

      val msg: SimulatedMessage = SimulatedMessage(System.currentTimeMillis(), DummyMessage.byteArray)

      JacksonMapper.mapper.writeValue(out, msg)
      val jsonMsg = out.toString()

      val rightBeforePublish = System.currentTimeMillis()

      channel.basicPublish(exchange, "", null, jsonMsg.getBytes())

      //println("rightBeforePublish = " + rightBeforePublish)
      println("Serialization took " + (rightBeforePublish - msg.timestamp) + " ms.")
      println("Message size = " + jsonMsg.size)
      println(jsonMsg)


    }
    case _ => {}
  }
}

/*

class ListeningActor(channel: Channel, queue: String) extends Actor {

  // called on the initial run
  def receive = {
    case _ => startReceving
  }

  def startReceving = {

    val consumer = new QueueingConsumer(channel)
    channel.basicConsume(queue, true, consumer)

    while (true) {
      // wait for the message

      val delivery = consumer.nextDelivery(10)

      if (delivery != null ) {
        val msg = new String(delivery.getBody())

        /*
        val receivedTime = System.currentTimeMillis()
        val simulatedMessage = JacksonMapper.mapper.readValue(msg, classOf[SimulatedMessage])
        val latency = receivedTime - simulatedMessage.timestamp

        println("Message " + simulatedMessage.timestamp + " received with latency " + latency + "ms.")
        */

        // and execute in a subactor
        context.actorOf(Props(new Actor {
          def receive = {
            case msg: String => {
              val receivedTime = System.currentTimeMillis()
              val simulatedMessage = JacksonMapper.mapper.readValue(msg, classOf[SimulatedMessage])
              val latency = receivedTime - simulatedMessage.timestamp

              //println("receivedTime = " + receivedTime)
              println("Message " + simulatedMessage.timestamp + " received with latency " + latency + "ms.")
            }
          }
        })) ! msg

      }


    }
  }
}
*/