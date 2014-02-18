package Hub.Subscription

import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.{Props, ActorSystem}
import org.scalatest._
import Hub.Subscription.TopicObserver.Done
import eventstore._
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.immutable.Queue
import org.scalatest.matchers.ShouldMatchers
import eventstore.EventRecord
import Hub.Subscription.TopicObserver.RemoveSubscriber
import Hub.Subscription.TopicObserver.NewSubscriber

object TopicObserverSpec {

  var received: Queue[Event] = Queue.empty

  def updateCallback(url: String, e: Event, topic: String): Future[Int] = {
    received = received enqueue(e)
    Future.successful(200)
  }

  def unSubscribeCallback(url: String, topic: String): Future[Int] = {
    Future.successful(200)
  }

  def fakeSubscriber(callback: String, unSubscribe: String, topic: String) = Props(new Subscriber(callback, unSubscribe, topic) {
    override def client = new FakeWebClient(updateCallback, unSubscribeCallback)
  })

  def buildTopicObserver(topic: String) = Props(new TopicObserver(topic) {
    override def subscriberProps(topic: String, callbackUrl: String, unSubscribeUrl: String): Props =
      fakeSubscriber(callbackUrl, unSubscribeUrl, topic)
  })

  def fakeEvent(data: String): EventRecord = new EventRecord(EventStream("id"), EventNumber(1), EventData(data))
}

class TopicObserverSpec extends TestKit(ActorSystem("TopicObserverSpec"))
  with WordSpec
  with BeforeAndAfterAll
  with ImplicitSender
  with ShouldMatchers {
    import TopicObserverSpec._


    "a topic observer" should {

      val topic = "http://localhost:2113/streams/UserEvents"
      val topicObserver = system.actorOf(buildTopicObserver(topic))

      "accept new subscriber urls" in {
        topicObserver ! NewSubscriber("callback_url_1", "unsubscribe")
        expectMsg(Done)
      }

      "remove a subscriber" in {
        topicObserver ! RemoveSubscriber("callback_url_1")
        expectMsg(Done)
      }

      "not publish events until live processing has started" in {
        topicObserver ! NewSubscriber("callback_url_2", "unsubscribe")
        topicObserver ! fakeEvent("data1")

        //assert that no event was collected
        received should be ('empty)

        topicObserver ! LiveProcessingStarted
        topicObserver ! fakeEvent("data2")

        //we need to wait here for the fake subscriber callback to work
        within(100 millis) {
          awaitAssert(received should have length (1))
        }
      }

      "should be able to receive subscriptions after live processing has started" in {
        topicObserver ! LiveProcessingStarted
        topicObserver ! NewSubscriber("callback_url_3", "unsubscribe")
        topicObserver ! fakeEvent("data3")

        within(100 millis) {
          awaitAssert(received.contains(fakeEvent("data3")))
        }
      }
    }

  override def afterAll() = {
    system.shutdown()
  }
}
