package Hub.Subscription

import akka.actor.{ActorLogging, Actor}
import Hub.Subscription.TopicPersistenceActor._
import akka.event.LoggingReceive
import Hub.Subscription.TopicPersistenceActor.NewTopic
import Hub.Subscription.TopicPersistenceActor.NewTopicResult
import Hub.Subscription.TopicPersistenceActor.GetAllTopicsResult

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

class TopicPersistenceActor() extends Actor with ActorLogging {

  def persistence: TopicPersistence = MongoTopicPersistence

  // we really want to make this asynchronous; need to use Futures or
  // use the reactivemongo driver
  def receive = LoggingReceive  {
    case n: NewTopic => {
      try {
        persistence.newTopic(n.topic)
        sender ! NewTopicResult(true, n.topic, null)
      } catch {
        case e: Exception => sender ! NewTopicResult(false, n.topic, e.getMessage)
      }
    }
    case GetAllTopics => sender ! GetAllTopicsResult(persistence.getAllTopics())
    case r: RemoveTopic => {
      try {
        persistence.removeTopic(r.topic)
        sender ! RemoveTopicResult(true, r.topic, null)
      } catch {
        case e: Exception => sender ! RemoveTopicResult(false, r.topic, e.getMessage)
      }
    }
    case _ => //error: message not recognized
  }

  override def preStart() = {
    log.debug("Starting persistence actor...")
  }

  override def preRestart(reason: Throwable, message: Option[Any]) {
    log.error(reason, "Restarting due to [{}] when processing [{}]",
      reason.getMessage, message.getOrElse(""))
  }
}
