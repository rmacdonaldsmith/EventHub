package Hub.Subscription

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
