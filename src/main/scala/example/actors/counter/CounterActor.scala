package example.actors.counter

import java.time.LocalDateTime

import akka.actor.ActorLogging
import akka.persistence.{PersistentActor, SnapshotOffer}
import example.actors.counter.CounterActor._

class CounterActor extends PersistentActor with ActorLogging {

  override def persistenceId: String = "counter-actor-persistence-id"

  private var state = CounterState()

  override def receiveRecover: Receive = {
    case event: Event =>
      log.info(s"recovering event: $event")
      eventHandler(event)
    case SnapshotOffer(metadata, snapshot: CounterState) =>
      state = snapshot
      log.info("State restored from snapshot")
  }
  override def receiveCommand: Receive = {
    case IncrementCmd(value, user) =>
      val event = IncrementedEvt(value, user, LocalDateTime.now().toString)
      persist(event)(eventHandler)
    case ReadValueCmd() =>
      sender() ! state

    case _ =>
      log.error("Unknown command received")
  }

  private val millis: Long = System.currentTimeMillis()
  private val eventHandler: Event => Unit = {
    case IncrementedEvt(value, user, timestamp) =>

      state = CounterState(state.count + value, Some(user), timestamp)
  }
}

object CounterActor {
  case class CounterState(count: Int = 0,
                          updatedBy: Option[String] = None,
                          date: String = LocalDateTime.now().toString)
}
