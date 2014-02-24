package Hub.Subscription

import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import akka.actor.{ActorLogging, Actor, Props, ActorSystem}
import org.scalatest._
import Hub.Subscription.TopicObserver.{RemoveAllSubscribers, Done, RemoveSubscriber, NewSubscriber}
import eventstore._
import scala.concurrent.duration._
import scala.collection.immutable.Queue
import org.scalatest.matchers.ShouldMatchers
import eventstore.EventRecord
import akka.event.LoggingReceive

object TopicObserverSpec {

  var received: Queue[Event] = Queue.empty
  var unSubscribed = Set[String]()

  class FakeSubscriber(callbackUrl: String, onUpdates: (Event) => Unit, onUnSubscribes: (String) => Unit) extends Actor with ActorLogging {
    def receive = LoggingReceive {
      case Subscriber.Update(payload) =>
        onUpdates(payload)
      case Subscriber.UnSubscribe => {
        onUnSubscribes(callbackUrl)
      }
    }
  }

  def doUpdates(e: Event) =
    received = received.enqueue(e)

  def doUnSubscribes(callbackUrl: String) = {
    println("Unsubscribing " + callbackUrl)
    unSubscribed += callbackUrl
  }

  def buildTopicObserver(topic: String) = Props(new TopicObserver(topic) {
    override def subscriberProps(topic: String, callbackUrl: String, unSubscribeUrl: String): Props =
      Props(new FakeSubscriber(callbackUrl, e => doUpdates(e), c => doUnSubscribes(c)))
      //fakeSubscriber(callbackUrl, unSubscribeUrl, topic)
  })

  def fakeEvent(eventNum: Int): EventRecord = new EventRecord(EventStream("streamId"), EventNumber(eventNum), EventData("data"))
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
        val observer = TestActorRef(buildTopicObserver(topic))
        observer ! NewSubscriber("callback_url_2", "unsubscribe")
        observer ! fakeEvent(1)

        //assert that no event was collected
        received should be ('empty)

        observer ! LiveProcessingStarted
        observer ! fakeEvent(2)

        within(100 millis) {
          awaitAssert(received should have length (1))
          assert(received.filter(_.number.value == 2).length == 1)
        }
      }

      "be able to receive subscriptions after live processing has started" in {
        val observer = TestActorRef(buildTopicObserver(topic))
        received = Queue.empty

        observer ! LiveProcessingStarted
        observer ! NewSubscriber("callback/url/update", "unsubscribe")
        expectMsg(Done)

        observer ! fakeEvent(3)

        within(1000 millis) {
          received should have length(1)
        }
      }

      "un-subscribe all clients when told to do so" in {
        val sub1 = "http://localhost1/updates"
        val sub2 = "http://localhost2/updates"
        val observer = TestActorRef(buildTopicObserver("topic"))
        observer ! NewSubscriber(sub1, "http://localhost1/unsubscribe")
        observer ! NewSubscriber(sub2, "http://localhost2/unsubscribe")

        expectMsg(Done)
        expectMsg(Done)

        observer ! RemoveAllSubscribers

        within(100 millis) {
          awaitCond(unSubscribed contains(sub1))
          assert(unSubscribed contains(sub2))
        }
      }

      "remove clients that fail when sending updates" in {
        import Subscriber.Failed
        val subscriber = "http://localhost/failingsubscriber/updates"
        val observer = TestActorRef(buildTopicObserver("topic"))

        observer ! LiveProcessingStarted
        observer ! NewSubscriber(subscriber, "unsubscribe")
        observer ! Failed(subscriber)

        within(100 millis) {
          awaitCond(unSubscribed contains(subscriber))
        }
      }
    }

  override def afterAll() = {
    system.shutdown()
  }
}
