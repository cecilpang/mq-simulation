import simulator.Simulator

object ConsumerApp extends App {

  Simulator.startConsumers(args(0), args(1), args(2).toInt)
}
