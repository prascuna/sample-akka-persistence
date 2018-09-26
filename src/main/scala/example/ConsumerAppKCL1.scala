package example

import java.util.UUID
import java.util.concurrent.{ExecutionException, TimeUnit, TimeoutException}

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{
  KinesisClientLibConfiguration,
  Worker
}
import example.actors.counter.CounterActor
import example.consumer.http.Routes
import example.consumerkcl1.RecordProcessorKCL1

import scala.concurrent.duration._

object ConsumerAppKCL1 extends App {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher

  val counterActor = system.actorOf(Props[CounterActor], "counter")
  implicit val askTimeout = Timeout(5.seconds)

  val routes = Routes(counterActor)

  val bindingFuture = Http().bindAndHandle(routes, "localhost", 8080)

  // AWS KCL
  System.setProperty("aws.cborEnabled", "false") // requited only for local
  val kinesisConfig = new KinesisClientLibConfiguration(
    "team55-team-application",
    "team55-test-stream",
    new DefaultAWSCredentialsProviderChain(),
    "team55-worker-id" + UUID.randomUUID().toString
  ).withKinesisEndpoint("https://kinesalite:4567")
    .withDynamoDBEndpoint("http://localhost:8000")

  val worker = new Worker.Builder()
    .recordProcessorFactory(RecordProcessorKCL1.factory(counterActor))
    .config(kinesisConfig)
    .build()

  val schedulerThread = new Thread(worker)
  schedulerThread.setDaemon(true)
  schedulerThread.start()

  system.registerOnTermination {
    try worker.startGracefulShutdown().get(20, TimeUnit.SECONDS)
    catch {
      case e: InterruptedException =>
        println("Interrupted while waiting for graceful shutdown. Continuing.")
      case e: ExecutionException =>
        println("Exception while executing graceful shutdown.", e)
      case e: TimeoutException =>
        println(
          "Timeout while waiting for shutdown.  Scheduler may not have exited."
        )
    }
    println("Scheduler shut down")
  }

}
