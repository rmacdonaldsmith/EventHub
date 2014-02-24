package Hub.Subscription

import scala.concurrent.duration._
import akka.actor._
import eventstore.{LiveProcessingStarted, Event}
import Hub.Subscription.TopicObserver.{RemoveAllSubscribers, RemoveSubscriber, Done, NewSubscriber}
import Hub.Subscription.Subscriber.{Failed, Update}
import akka.event.LoggingReceive

object TopicObserver {
  //val connection = context.actorOf(ConnectionActor.props(), "es-connection")
  //val topicSubscription = context.actorOf(Props[TopicObserver])
  //context.actorOf(StreamSubscriptionActor.props(connection, topicSubscription, EventStream("UserEvents")))
  case class NewSubscriber(callbackUrl: String, unSubscribeUrl: String)
  case class RemoveSubscriber(callbackUrl: String)
  object RemoveAllSubscribers
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

  def waiting(): Receive = LoggingReceive {
    case LiveProcessingStarted =>
      context become liveProcessing()

    case n: NewSubscriber =>
      newSubscriber(n)
      sender ! Done

    case r: RemoveSubscriber =>
      removeSubscriber(r.callbackUrl)
      sender ! Done

    case RemoveAllSubscribers =>
      removeAllSubscribers
      sender ! Done
  }

  def liveProcessing(): Receive = LoggingReceive {
    case e: Event =>
      subscribers foreach {subscriber => subscriber._2 ! Update(e)}

    case Subscriber.Failed(url) =>
      log.warning("Failed to post update to [{}] - un-subscribing this client", url)
      removeSubscriber(url)

    case n: NewSubscriber =>
      newSubscriber(n)
      sender ! Done

    case r: RemoveSubscriber =>
      removeSubscriber(r.callbackUrl)
      sender ! Done

    case RemoveAllSubscribers =>
      removeAllSubscribers
      sender ! Done

    case Failed(url) =>
      removeSubscriber(url)
  }

  def newSubscriber(n: NewSubscriber) = {
    val subscriberRef = context.actorOf(subscriberProps(topic, n.callbackUrl, n.unSubscribeUrl), n.callbackUrl.replace('/', '_'))
    subscribers += (n.callbackUrl -> subscriberRef)
    //context watch subscriberRef //death watch - need to handle this!
  }

  def removeSubscriber(url: String): Unit = {
    subscribers get url match {
      case Some(subscriber) =>
        subscriber ! Subscriber.UnSubscribe
        subscribers -= url
      case None =>
        log.warning("Subscriber [{}] not found in list of registered subscribers.", url)
    }
  }

  def removeAllSubscribers = {
    subscribers foreach (_._2 ! Subscriber.UnSubscribe)
  }
}
