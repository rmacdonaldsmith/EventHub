package Hub.Subscription

import akka.actor.{Props, Actor}
import Hub.Subscription.Subscriber.{Failed, Update, UnSubscribe}
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executor
import eventstore.Event

object Subscriber {
  object UnSubscribe
  case class Update(payload: Event)
  case class Failed(callbackUrl: String)
}

class Subscriber(callbackUrl: String, unSubscribeUrl: String, topic: String) extends Actor{

  implicit val executor = context.dispatcher.asInstanceOf[Executor with ExecutionContext]
  def client: WebClient = AsyncWebClient
  
  def receive = {
    case Update(payload) =>
      doUpdate(payload)
    case UnSubscribe =>
      doUnSubscribe
    case Failed(clientUrl) =>
      //log?
  }

  def doUpdate(payload: Event): Unit = {
    val future = client.postUpdate(callbackUrl, payload, topic)
    future onFailure  {
      //need to check that i should handle a throwable - does the Akka runtime need to handle throwables?
      case err: Throwable => sender ! Failed(callbackUrl)
    }
  }

  def doUnSubscribe: Unit = {
    //tell the client that they have been un-subscribed
    val future = client.postUnSubscribe(unSubscribeUrl, topic)
    future onFailure {
      case err: Throwable => //log
    }
  }
}
