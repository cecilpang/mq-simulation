package actor

import akka.actor.{ActorSystem, Actor}
import com.rabbitmq.client.Channel
import simulator.{JacksonMapper, DummyMessage, SimulatedMessage}
import java.io.StringWriter

object AppActorSystem {
  // Create the 'mq-simulation' actor system
  val actorSystem = ActorSystem("mq-simulation")
}

class PublishingActor(channel: Channel, exchange: String) extends Actor {

  /**
   * When we receive a message we sent it using the configured channel
   */
  def receive = {
    case some: String => {

      val out = new StringWriter

      val msg: SimulatedMessage = SimulatedMessage(System.currentTimeMillis(), DummyMessage.byteArray)

      JacksonMapper.mapper.writeValue(out, msg)
      val jsonMsg = out.toString()

      val rightBeforePublish = System.currentTimeMillis()

      channel.basicPublish(exchange, "", null, jsonMsg.getBytes())

      println(jsonMsg.substring(0, 100) + "...")
      println("Message size = " + jsonMsg.size)
      println("Serialization took " + (rightBeforePublish - msg.timestamp) + " ms.")


    }
    case _ => {}
  }
}
