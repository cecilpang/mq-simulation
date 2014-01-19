package actor

import akka.actor.{Props, ActorSystem, Actor}
import com.rabbitmq.client.{QueueingConsumer, Channel}

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
      val msg = System.currentTimeMillis().toString
      channel.basicPublish(exchange, "", null, msg.getBytes())
      println(msg)
    }
    case _ => {}
  }
}

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
      val delivery = consumer.nextDelivery()
      val msg = new String(delivery.getBody())

      // and execute in a subactor
      context.actorOf(Props(new Actor {
        def receive = {
          case some: String => {
            val receivedTime = System.currentTimeMillis()
            val latency = receivedTime - some.toLong

            println("Message " + some + " received with latency " + latency + "ms.")
          }
        }
      })) ! msg
    }
  }
}