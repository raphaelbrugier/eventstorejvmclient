package eventstore

import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import akka.testkit._
import akka.actor.{ Props, SupervisorStrategy, Actor, ActorSystem }
import scala.concurrent.duration._

/**
 * @author Yaroslav Klymko
 */
abstract class AbstractCatchUpSubscriptionActorSpec extends Specification with Mockito {

  abstract class AbstractScope extends TestKit(ActorSystem()) with ImplicitSender with Scope {
    val duration = FiniteDuration(1, SECONDS)
    val readBatchSize = 10
    val resolveLinkTos = false
    val connection = TestProbe()

    class Supervisor extends Actor {
      override def supervisorStrategy = SupervisorStrategy.stoppingStrategy
      def receive = PartialFunction.empty
    }

    val actor = {
      val supervisor = TestActorRef(new Supervisor)
      val actor = TestActorRef(Props(newActor), supervisor, "test")
      watch(actor)
      actor
    }

    def newActor: Actor

    def streamId: EventStream

    def expectNoActivity {
      expectNoMsg(duration)
      connection.expectNoMsg(duration)
    }

    def streamEventAppeared(x: Event) = StreamEventAppeared(IndexedEvent(x, Position(x.number.value)))

    def subscribeTo = SubscribeTo(streamId, resolveLinkTos = resolveLinkTos)

    def expectActorTerminated(testKit: TestKitBase = this) {
      testKit.expectTerminated(actor)
      actor.underlying.isTerminated must beTrue
      expectNoActivity
    }
  }

}
