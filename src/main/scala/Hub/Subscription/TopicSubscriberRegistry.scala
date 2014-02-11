package Hub.Subscription

import akka.actor.{Props, Actor, ActorLogging}

object TopicSubscriberRegistry {
  def buildTestTopicPersistence(): Props =
    Props()
}

class TopicSubscriberRegistry extends Actor with ActorLogging {

  def receive = {
    case _ => //log
  }
}
