package Hub.Subscription

import scala.concurrent.duration._
import akka.actor._
import Hub.Subscription.TopicPersistenceActor._
import akka.event.LoggingReceive

object TopicPersistenceActor {

  trait PersistenceOperation {
    def client: ActorRef
  }

  trait WriteOperation extends PersistenceOperation {
    def topic: String
  }

  trait Result
  
  case class NewTopic(client: ActorRef, topic: String) extends WriteOperation

  case class WriteResult(ok: Boolean, topic: String, err: String) extends Result

  case class RemoveTopic(client: ActorRef, topic: String) extends WriteOperation

  case class GetAllTopics(client: ActorRef) extends PersistenceOperation

  case class GetAllTopicsResult(topics: Iterable[String]) extends Result
}

class TopicPersistenceActor() extends Actor with ActorLogging {

  def persistence: TopicPersistence = MongoTopicPersistence
  def workerProps: Props = Props(new PersistenceWorker(persistence))
  var requestNumber: Int = 0

  // we really want to make this asynchronous; need to use Futures or
  // use the reactivemongo driver

  def receive = waiting

  def waiting: Receive = LoggingReceive {
    case p: PersistenceOperation =>
      log.debug("Moving from waiting to runNext...")
      context.become(runNext(Vector[PersistenceOperation](p)))
  }

  def runNext(queue: Vector[PersistenceOperation]): Receive = LoggingReceive {
    requestNumber += 1
    if(queue.isEmpty) { log.debug("Queue empty, entering waiting..."); waiting; }
    else {
      log.debug("Queue not empty: " + queue.length)
      val worker = context.actorOf(workerProps, s"worker$requestNumber")
      worker ! queue.head
      context.setReceiveTimeout(3 seconds)
      running(queue)
    }
  }

  def running(queue: Vector[PersistenceOperation]): Receive = LoggingReceive {
    case r: Result =>
      val op = queue.head
      op.client ! r
      log.debug("Result received. Operations in the queue: " + queue.length)
      //send poison pill to worker? Currently, worker commits suicide when it is done sending a result
      context.become(runNext(queue.tail))
    case w: PersistenceOperation =>
      log.debug("Received persistence operation. Queue: " + queue.length)
      context.become(enqueue(queue, w))
    case ReceiveTimeout => //we did not get a response in time. need to set the timeout
    case Terminated     => //death watch (or not since worker will suicide when done - need to send poison pill?)
  }

  def enqueue(queue: Vector[PersistenceOperation], op: PersistenceOperation): Receive = {
    running(queue :+ op)
  }

  override def preStart() = {
    log.debug("Starting persistence actor...")
  }

  override def preRestart(reason: Throwable, message: Option[Any]) {
    log.error(reason, "Restarting due to [{}] when processing [{}]",
      reason.getMessage, message.getOrElse(""))
  }
}
