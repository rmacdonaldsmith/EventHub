package Hub.Subscription

import akka.actor.{ActorRef, Props}
import scala.concurrent.Future
import scala.concurrent.duration._
import org.specs2.mutable._
import Hub.Subscription.SubscriberSpec.FakeWebClient

object SubscriberSpec {

  def buildTestSubscriber(url: String, unSubscribeUrl: String, topic: String, webClient: WebClient): Props =
    Props(new Subscriber(url, unSubscribeUrl, topic) {
      override def client = webClient
    })

  class FakeWebClient(updateCallback: (String, Any, String) => Future[Int], unSubscribeCallback: (String, String) => Future[Int]) extends WebClient {
    override def postUpdate(url: String, payload: Any, topic: String): Future[Int] = updateCallback(url, payload, topic)
    override def postUnSubscribe(url: String, topic: String): Future[Int] = unSubscribeCallback(url, topic)
  }
}

trait WebClientScope extends Specs2ActorContext with After {
  var called = false

  def fakeWebClient = {
    new FakeWebClient((url, payload, topic) => {
      called = true
      Future.successful(204)
    },
      (url, topic) => {
        called = true
        Future.successful(204)
      })
  }

  override def after = called = false
}

class SubscriberSpec extends Specification {

  import SubscriberSpec._

  "A subscriber" should {

    "forward the new event to the callback url" in new WebClientScope {
      val callbackUrl = "http://localhost:9000/UserEvents"
      val subscriber: ActorRef = system.actorOf(buildTestSubscriber(callbackUrl, "unSubscribeUrl", "aTopic", fakeWebClient))

      subscriber ! Subscriber.Update(Nil)

      within(200 milli) {
        expectNoMsg
        called must beTrue
      }
    }

    "call the unsubscriber url" in new WebClientScope {
      val unsubscribeUrl = "http://localhost:9000/UnSubscribe"
      val subscriber: ActorRef = system.actorOf(buildTestSubscriber("callbackUrl", unsubscribeUrl, "aTopic", fakeWebClient))

      subscriber ! Subscriber.UnSubscribe

      within(200 milli) {
        expectNoMsg
        called must beTrue
      }
    }
  }
}




