package Hub.Subscription

import akka.actor.Actor
import Hub.Subscription.Subscriber.{Failed, Update, UnSubscribe}
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executor

object Subscriber {
  object UnSubscribe
  case class Update(payload: Any)
  case class Failed(callbackUrl: String)
}

class Subscriber(callbackUrl: String, unSubscribeUrl: String, topic: String) extends Actor{

  implicit val executor = context.dispatcher.asInstanceOf[Executor with ExecutionContext]
  def client: WebClient = AsyncWebClient
  
  def receive = {
    case Update(payload) => doUpdate(payload)
    case UnSubscribe => doUnSubscribe
    case Failed(clientUrl) => //log?
  }

  def doUpdate(payload: Any): Unit = {
    val future = client.postUpdate(callbackUrl, payload, topic)
    future onFailure  {
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
