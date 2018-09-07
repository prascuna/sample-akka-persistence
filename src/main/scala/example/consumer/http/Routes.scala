package example.consumer.http

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import akka.pattern._
import akka.util.Timeout
import example.actors.counter.CounterActor.CounterState
import example.actors.counter.{IncrementCmd, ReadValueCmd}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class Routes(counterActor: ActorRef)(implicit ec: ExecutionContext)
    extends Route {
  private implicit val askTimeout: Timeout = Timeout(5.seconds)
  override def apply(requestContext: RequestContext): Future[RouteResult] = {
    path("increase") {
      get {
        onSuccess(counterActor ? ReadValueCmd()) {
          case CounterState(count, updatedBy, date) =>
            complete(
              OK,
              s"Count: $count | updatedBy: $updatedBy | updatedAt: $date"
            )
        }
      } ~
        post {
          counterActor ! IncrementCmd(1, "fake user")
          complete(OK, "Updated")
        }
    }
  }.apply(requestContext)
}
object Routes {
  def apply(counterActor: ActorRef)(implicit ec: ExecutionContext): Routes =
    new Routes(counterActor)
}
