package Hub.Subscription

import akka.actor.{Actor, Props, ActorRef}

//used for testing - takes the place of the parent of an actor on which you want to assert that messages are received.
class StepParent(child: Props, fwd: ActorRef) extends Actor {
  context.actorOf(child, "child")
  def receive = {
    case msg => fwd.tell(msg, sender)
  }
}
