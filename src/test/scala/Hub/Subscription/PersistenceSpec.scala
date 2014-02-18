package Hub.Subscription

import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.{Props, ActorSystem}
import org.scalatest.{WordSpec, BeforeAndAfterAll}
import Hub.Subscription.TopicPersistenceActor._


object PersistenceSpec {
  def buildPersistenceActor(fakePersistence: TopicPersistence): Props =
    Props(new TopicPersistenceActor {
      override def persistence = fakePersistence
    })

  class FakePersistence extends TopicPersistence {
    var topics: Map[String, Set[String]] = Map()

    override def getTopicsFor(subscriberUrl: String): Iterable[String] = ???

    override def getAllSubscribersFor(topic: String): Iterable[String] =
      topics get topic match {
        case Some(set) => set
        case None => Nil
      }

    override def getAllTopics(): Iterable[String] = topics.keys

    override def removeSubscriber(subscriberUrl: String, topic: String): Unit = ???

    override def addSubscriber(subscriberUrl: String, topic: String): Unit = ???

    override def removeTopic(topic: String): Unit =
      if (topics.contains(topic) == false) throw new Exception("Topic does not exist")
      else topics -= topic

    override def newTopic(topic: String): Unit =
      if (topics.contains(topic)) throw new Exception("Topic already exists")
      else topics += topic -> Set[String]()
  }
}

class PersistenceSpec extends TestKit(ActorSystem("PersistenceSpec"))
  with WordSpec
  with BeforeAndAfterAll
  with ImplicitSender {

  import PersistenceSpec._

  "A topic persistence actor" should {

    val topicPersistence = system.actorOf(buildPersistenceActor(new FakePersistence), "TopicPersistenceActor")

    "persist a new topic" in {

      topicPersistence ! NewTopic(testActor, "http://hostname:2113/blah/events")
      expectMsg(WriteResult(true, "http://hostname:2113/blah/events", null))

      topicPersistence ! GetAllTopics(testActor)
      expectMsg(GetAllTopicsResult(Set("http://hostname:2113/blah/events")))
    }

    "not accept a duplicate topic" in {
      topicPersistence ! NewTopic(testActor, "http://hostname:2113/blah/events")
      expectMsg(WriteResult(false, "http://hostname:2113/blah/events", "Topic already exists"))
    }

    "remove a topic" in {
      topicPersistence ! RemoveTopic(testActor, "http://hostname:2113/blah/events")
      expectMsg(WriteResult(true, "http://hostname:2113/blah/events", null))

      topicPersistence ! GetAllTopics(testActor)
      expectMsg(GetAllTopicsResult(Set()))
    }

    "tell you when removing a non-existent topic" in {
      topicPersistence ! RemoveTopic(testActor, "http://hostname:2113/bad/events")
      expectMsg(new WriteResult(false, "http://hostname:2113/bad/events", "Topic does not exist"))
    }

    "handle requests in the order that they were sent" in {
      for (i <- 1 to 5) topicPersistence ! NewTopic(testActor, s"http://hostname:2113/blah/events$i")
      for (i <- 1 to 5) expectMsg(WriteResult(true, s"http://hostname:2113/blah/events$i", null))
    }

    "receive a failed response when the worker persistence fails" in {

    }

    "timeout when no response is received from the worker" in {

    }
  }

  override def afterAll() = {
    system.shutdown()
  }
}
