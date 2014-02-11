package Hub.Subscription

import scala.concurrent.duration._
import akka.actor.{ActorRef, Props}
import Hub.Subscription.TopicPersistenceActor.{GetAllTopicsResult, NewTopicResult}
import scala.collection.mutable.Set

object TopicPersistenceSpec {
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

    override def removeTopic(topic: String): Unit = topics -= topic

    override def newTopic(topic: String): Unit =
      if (topics.contains(topic)) throw new Exception("Topic already exists")
      else topics updated (topic, Set[String]())
  }
}

//class TopicPersistenceSpec extends ActorSpec {
//
//  import TopicPersistenceSpec._
//
//  //val topicPersistence = system.actorOf(Props(new StepParent(buildPersistenceActor(new FakePersistence), testActor)))
//
//  "A topic persistence actor" should {
//
//    "persist a new topic" in new ActorScope {
//      val topicPersistence = system.actorOf(Props(new StepParent(buildPersistenceActor(new FakePersistence), testActor)))
//      //val topicPersistence: ActorRef = system.actorOf(buildPersistenceActor(new FakePersistence))
//      topicPersistence ! TopicPersistenceActor.NewTopic("http://hostname:2113/blah/events")
//
//      within(100 milli) {
//        expectMsg(new NewTopicResult(true, "http://hostname:2113/blah/events", null))
//      }
//
//      topicPersistence ! TopicPersistenceActor.GetAllTopics
//
//      within(100 milli) {
//        expectMsg(new GetAllTopicsResult(Seq("http://hostname:2113/blah/events")))
//      }
//    }
//
////    "contain the new topic" in new ActorScope {
////      topicPersistence ! TopicPersistenceActor.GetAllTopics
////
////      within(100 milli) {
////        expectMsg(TopicPersistenceActor.GetAllTopicsResult(Seq("http://hostname:2113/blah/events")))
////      }
////    }
//
//    "not accept a duplicate topic" in new ActorScope {
//      //topicPersistence ! TopicPersistenceActor.NewTopic("http://hostname:2113/foo/events")
//
//      within(100 milli) {
//        expectMsg(new NewTopicResult(false, "http://hostname:2113/blah/events", "Topic already exists"))
//      }
//    }
//
//    "remove a topic" in new ActorScope {
//      //topicPersistence ! TopicPersistenceActor.RemoveTopic("http://hostname:2113/blah/events")
//
//      within(100 milli) {
//        expectMsg(new TopicPersistenceActor.RemoveTopicResult(true, "http://hostname:2113/blah/events", null))
//      }
//    }
//
//    "not puke when removing a non-existent topic" in new ActorScope {
//      //topicPersistence ! TopicPersistenceActor.RemoveTopic("http://hostname:2113/bad/events")
//
//      within(100 milli) {
//        expectMsg(new TopicPersistenceActor.RemoveTopicResult(false, "http://hostname:2113/bad/events", "Topic does not exist"))
//      }
//    }
//  }
//}
