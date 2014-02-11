package Hub.Subscription

import akka.actor.Actor
import Hub.Subscription.TopicPersistenceActor.{NewTopicResult, NewTopic}

trait TopicPersistence {
  def newTopic(topic: String): Unit

  def removeTopic(topic: String): Unit

  def addSubscriber(subscriberUrl: String, topic: String): Unit

  def removeSubscriber(subscriberUrl: String, topic: String): Unit

  def getAllTopics(): Iterable[String]

  def getAllSubscribersFor(topic: String): Iterable[String]

  def getTopicsFor(subscriberUrl: String): Iterable[String]
}

//i want to use the reactivemongo driver to keep everything in this project asynchronous.
//but it seems that I need a newer version of the play framework, so opted for the official
//and blocking Mongo driver instead.
object MongoTopicPersistence extends TopicPersistence {
  import com.mongodb.casbah.Imports._

  val mongoClient = MongoClient("localhost", 27017)
  val db = mongoClient("EventHub")
  val collection = db("TopicSubscribers")

  override def getTopicsFor(subscriberUrl: String): Iterable[String] = ???

  override def getAllSubscribersFor(topic: String): Iterable[String] = ???

  override def getAllTopics(): Iterable[String] = ???

  override def removeSubscriber(subscriberUrl: String, topic: String): Unit = ???

  override def addSubscriber(subscriberUrl: String, topic: String): Unit = ???

  override def removeTopic(topic: String): Unit = ???

  override def newTopic(topic: String): Unit = ???
}

object TopicPersistenceActor {

  trait WriteOperation {
    def topic: String
  }

  case class NewTopic(topic: String) extends WriteOperation

  case class NewTopicResult(ok: Boolean, topic: String, err: String) extends WriteOperation

  case class RemoveTopic(topic: String) extends WriteOperation

  case class RemoveTopicResult(ok: Boolean, topic: String, err: String) extends WriteOperation

  object GetAllTopics

  case class GetAllTopicsResult(topics: Iterable[String])
}

class TopicPersistenceActor() extends Actor {

  def persistence: TopicPersistence = MongoTopicPersistence

  // we really want to make this asynchronous; need to use Futures or
  // use the reactivemongo driver
  def receive = {
    case n: NewTopic => {
      try {
        persistence.newTopic(n.topic)
        context.parent ! NewTopicResult(true, n.topic, null)
      } catch {
        case e: Exception => context.parent ! NewTopicResult(false, n.topic, e.getMessage)
      }
    }
    case _ => //error: message not recognized
  }
}
