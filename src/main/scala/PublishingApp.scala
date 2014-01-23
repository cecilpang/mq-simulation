import simulator.Simulator

object PublishingApp extends App {
  Simulator.startPublishing(args(0), args(1), args(2).toInt)
}
