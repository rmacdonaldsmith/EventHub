package Hub.Subscription

import scala.concurrent.duration._
import akka.actor._
import eventstore.{LiveProcessingStarted, Event}
import Hub.Subscription.TopicObserver.{RemoveSubscriber, Done, NewSubscriber}
import Hub.Subscription.Subscriber.Update

object TopicObserver {
  //val connection = context.actorOf(ConnectionActor.props(), "es-connection")
  //val topicSubscription = context.actorOf(Props[TopicObserver])
  //context.actorOf(StreamSubscriptionActor.props(connection, topicSubscription, EventStream("UserEvents")))
  case class NewSubscriber(callbackUrl: String, unSubscribeUrl: String)
  case class RemoveSubscriber(callbackUrl: String)
  object Done

  def buildTopicObserver(topic: String): Props = Props(new TopicObserver(topic))
}

class TopicObserver(topic: String) extends Actor with ActorLogging {

  //we will override this factory method in tests
  def subscriberProps(topic: String, callbackUrl: String, unSubscribeUrl: String): Props =
    Props(new Subscriber(callbackUrl, unSubscribeUrl, topic))

  var subscribers: Map[String, ActorRef] = Map.empty

  //the sender should be the Subscription actor - we are subscribed to a stream from the connection via the StreamSubscriptionActor
  def receive = waiting()

  def waiting(): Receive = {
    case LiveProcessingStarted =>
      context become liveProcessing()

    case n: NewSubscriber =>
      newSubscriber(n)
      sender ! Done

    case r: RemoveSubscriber =>
      removeSubscriber(r)
      sender ! Done
  }

  def liveProcessing(): Receive = {
    case e: Event =>
      subscribers foreach {subscriber => subscriber._2 ! Update(e)}

    case Subscriber.Failed(url) => //do something with the subscriber that had a failed update...
      //probably want to unsubscribe them
      //log.warning("Failed to post update to [{}]", url)

    case n: NewSubscriber =>
      newSubscriber(n)
      sender ! Done

    case r: RemoveSubscriber =>
      removeSubscriber(r)
      sender ! Done
  }

  def newSubscriber(n: NewSubscriber) = {
    val subscriberRef = context.actorOf(subscriberProps(n.callbackUrl, n.unSubscribeUrl, topic), n.callbackUrl)
    context watch subscriberRef //death watch - need to handle this!
    subscribers += (n.callbackUrl -> subscriberRef)
  }

  def removeSubscriber(r: RemoveSubscriber): Any = {
    subscribers get r.callbackUrl match {
      case Some(subscriber) =>
        subscriber ! Subscriber.UnSubscribe
        subscribers -= r.callbackUrl
      case None => return
    }
  }
}
