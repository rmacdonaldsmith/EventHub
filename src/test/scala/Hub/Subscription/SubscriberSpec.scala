package Hub.Subscription

import akka.actor.{ActorRef, Props}
import scala.concurrent.Future
import scala.concurrent.duration._
import org.specs2.mutable._

object SubscriberSpec {

  def buildTestSubscriber(url: String, unSubscribeUrl: String, topic: String, webClient: WebClient): Props =
    Props(new Subscriber(url, unSubscribeUrl, topic) {
      override def client = webClient
    })

  object FakeWebClient extends WebClient {
    override def postUpdate(url: String, payload: Any, topic: String): Future[Int] = Future.successful(201)
    override def postUnSubscribe(url: String, topic: String): Future[Int] = Future.failed(PostUpdateFailed(500))
  }
}

class SubscriberSpec extends Specification {

  import SubscriberSpec._
  
  "A subscriber" should {

    "forward the new event to the callback url" in new Specs2Context {
      val fakeClient = FakeWebClient
      val callbackUrl = "http://localhost:9000/UserEvents"
      val subscriber: ActorRef = system.actorOf(buildTestSubscriber(callbackUrl, "unSubscribeUrl", "aTopic", fakeClient))

      subscriber ! Subscriber.Update(Nil)

      within(200 milli) {
        expectNoMsg
      }
    }

    "call the unsubscriber url" in new Specs2Context {
      
    }
  }
}




