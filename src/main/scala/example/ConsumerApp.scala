package example
import java.net.URI
import java.util.UUID
import java.util.concurrent.{ExecutionException, TimeUnit, TimeoutException}

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import example.actors.counter.CounterActor
import example.consumer.http.Routes
import example.consumer.kinesis.SampleShardRecordProcessor
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.http.Protocol
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.kinesis.common.{ConfigsBuilder, InitialPositionInStream, InitialPositionInStreamExtended}
import software.amazon.kinesis.coordinator.Scheduler

import scala.concurrent.duration._

object ConsumerApp extends App {

  System.setProperty("com.amazonaws.sdk.disableCertChecking", "1")// required only for local

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher

  val counterActor = system.actorOf(Props[CounterActor], "counter")
  implicit val askTimeout = Timeout(5.seconds)

  val routes = Routes(counterActor)

  val bindingFuture = Http().bindAndHandle(routes, "localhost", 8080)

  // AWS KCL
  val dynamoClient = DynamoDbAsyncClient
    .builder()
    .region(Region.US_EAST_1)
    .endpointOverride(new URI("http://localhost:8000"))
//    .endpointOverride(new URI("https://dynamodb.us-east-1.amazonaws.com"))
    .build()
  val cloudWatchClient = CloudWatchAsyncClient
    .builder()
    .region(Region.US_EAST_1)
    .endpointOverride(new URI("http://localhost:1234")) // does not exist
    .build()
  System.setProperty("aws.cborEnabled", "false") // required only for local
  val kinesisClient = KinesisAsyncClient
    .builder()
    .region(Region.US_EAST_1)
    .httpClient(NettyNioAsyncHttpClient.builder().protocol(Protocol.HTTP1_1).build())
    .endpointOverride(new URI("https://kinesalite:4567"))
//    .endpointOverride(new URI("https://localhost:4567"))
    .build()
  val configsBuilder = new ConfigsBuilder(
    "team55-test-stream",
    "team55-test-application",
    kinesisClient,
    dynamoClient,
    cloudWatchClient,
    "team55-worker-id" + UUID.randomUUID().toString,
    SampleShardRecordProcessor.factory
  )
  val scheduler = new Scheduler(
    configsBuilder.checkpointConfig(),
    configsBuilder.coordinatorConfig(),
    configsBuilder.leaseManagementConfig(),
    configsBuilder.lifecycleConfig(),
    configsBuilder.metricsConfig(),
    configsBuilder.processorConfig(),
    configsBuilder
      .retrievalConfig()
      .initialPositionInStreamExtended(
        InitialPositionInStreamExtended
          .newInitialPosition(InitialPositionInStream.TRIM_HORIZON)
      )
  )

  val schedulerThread = new Thread(scheduler)
  schedulerThread.setDaemon(true)
  schedulerThread.start()

  system.registerOnTermination {
    try scheduler.startGracefulShutdown().get(20, TimeUnit.SECONDS)
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
