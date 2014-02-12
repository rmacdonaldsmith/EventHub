package Hub.Subscription

import akka.actor.{Actor, ActorLogging}
import Hub.Subscription.TopicPersistenceActor._
import akka.event.LoggingReceive

trait TopicPersistence {
  def newTopic(topic: String): Unit

  def removeTopic(topic: String): Unit

  def addSubscriber(subscriberUrl: String, topic: String): Unit

  def removeSubscriber(subscriberUrl: String, topic: String): Unit

  def getAllTopics(): Iterable[String]

  def getAllSubscribersFor(topic: String): Iterable[String]

  def getTopicsFor(subscriberUrl: String): Iterable[String]
}

class PersistenceWorker(persistence: TopicPersistence) extends Actor with ActorLogging {

  def receive = LoggingReceive {
    case n: NewTopic => doNewTopic(n)
    case r: RemoveTopic => removeTopic(r)
    case g: GetAllTopics => getAllTopics(g)

    //when done processing a message, we die
    context.stop(self)
  }

  def getAllTopics(g: GetAllTopics) = {
    try {
      context.parent ! GetAllTopicsResult(persistence.getAllTopics())
    } catch {
      case e: Exception => context.parent ! WriteResult(false, null, null)
    }
  }

  def doNewTopic(n: NewTopic) = {
    try {
      persistence.newTopic(n.topic)
      context.parent ! WriteResult(true, n.topic, null)
    } catch {
      case e: Exception => context.parent ! WriteResult(false, n.topic, e.getMessage)
    }
  }

  def removeTopic(r: RemoveTopic) = {
    try {
      persistence.removeTopic(r.topic)
      context.parent ! WriteResult(true, r.topic, null)
    } catch {
      case e: Exception => context.parent ! WriteResult(false, r.topic, e.getMessage)
    }
  }

}
