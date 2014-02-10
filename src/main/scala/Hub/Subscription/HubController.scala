package Hub.Subscription

import akka.actor.{Actor, ActorLogging}
import Hub.Subscription.HubController.NewSubscriber

object HubController {
  class AddTopic(topic: String)
  class RemoveTopic(topic: String)
  class NewSubscriber(callbackUrl: String, topic: String)
  class UnSubscribe(callbackUrl: String, topic: String)
  object Result
}

class HubController extends Actor with ActorLogging{

  def receive = {
    case n: NewSubscriber => doSubscription(n)
    case _ => //log
  }

  def doSubscription(n: NewSubscriber) = {

  }
}
