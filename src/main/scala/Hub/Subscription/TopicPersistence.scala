package Hub.Subscription

import akka.actor.Actor


trait TopicPersistence {
  def newTopic(topic: String): Unit

  def removeTopic(topic: String): Unit

  def addSubscriber(subscriberUrl: String, topic: String): Unit

  def removeSubscriber(subscriberUrl: String, topic: String): Unit

  def getAllTopics(): Seq[String]

  def getAllSubscribersFor(topic: String): Seq[String]

  def getTopicsFor(subscriberUrl: String): Seq[String]
}

object MongoTopicPersistence extends TopicPersistence {
  import com.mongodb.casbah.Imports._

  val mongoClient = MongoClient("localhost", 27017)
  val db = mongoClient("EventHub")
  val collection = db("TopicSubscribers")

  override def getTopicsFor(subscriberUrl: String): Seq[String] = ???

  override def getAllSubscribersFor(topic: String): Seq[String] = ???

  override def getAllTopics(): Seq[String] = ???

  override def removeSubscriber(subscriberUrl: String, topic: String): Unit = ???

  override def addSubscriber(subscriberUrl: String, topic: String): Unit = ???

  override def removeTopic(topic: String): Unit = ???

  override def newTopic(topic: String): Unit = ???
}

class TopicPersistenceActor() extends Actor {

  def persistence: TopicPersistence = MongoTopicPersistence

  def receive = {
    case _ => //error: message not recognized
  }
}
