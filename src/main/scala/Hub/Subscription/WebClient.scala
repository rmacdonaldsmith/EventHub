package Hub.Subscription

import scala.concurrent.{Promise, Future}
import com.ning.http.client.{AsyncCompletionHandler, AsyncHttpClient, Response}
import eventstore.Event


trait WebClient {
  def postUpdate(url: String, payload: Event, topic: String): Future[Int]
  def postUnSubscribe(url: String, topic: String): Future[Int]
}

case class PostUpdateFailed(status: Int) extends RuntimeException

object AsyncWebClient extends WebClient {

  private val client = new AsyncHttpClient

  override def postUpdate(url: String, payload: Event, topic: String): Future[Int] = {
    val request = client.preparePost(url).build()
    val result = Promise[Int]()
    client.executeRequest(request, new AsyncCompletionHandler[Response]() {
      override def onCompleted(response: Response) = {
        if (response.getStatusCode / 100 < 4)
          result.success(response.getStatusCode)
        else
          result.failure(PostUpdateFailed(response.getStatusCode))
        response
      }

      override def onThrowable(t: Throwable) {
        result.failure(t)
      }
    })
    result.future
  }

  override def postUnSubscribe(url: String, topic: String): Future[Int] = {
    val request = client.preparePost(url).build()
    val result = Promise[Int]
    client.executeRequest(request, new AsyncCompletionHandler[Response] {
      override def onCompleted(response: Response) = {
        if (response.getStatusCode / 100 < 4)
          result.success(response.getStatusCode)
        else
          result.failure(PostUpdateFailed(response.getStatusCode))
        response
      }

      override def onThrowable(t: Throwable) {
        result.failure(t)
      }
    })
    result.future
  }

  def shutdown(): Unit = client.close()
}
