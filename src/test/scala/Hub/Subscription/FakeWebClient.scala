package Hub.Subscription

import scala.concurrent.Future
import eventstore.Event

class FakeWebClient(updateCallback: (String, Event, String) => Future[Int], unSubscribeCallback: (String, String) => Future[Int]) extends WebClient {
  override def postUpdate(url: String, payload: Event, topic: String): Future[Int] = updateCallback(url, payload, topic)
  override def postUnSubscribe(url: String, topic: String): Future[Int] = unSubscribeCallback(url, topic)
}
