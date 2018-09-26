package example
import java.util.UUID

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.{
  AmazonDynamoDB,
  AmazonDynamoDBClientBuilder
}
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{
  InitialPositionInStream,
  KinesisClientLibConfiguration,
  Worker
}
import com.amazonaws.services.kinesis.metrics.impl.NullMetricsFactory
import com.amazonaws.services.kinesis.model.StreamStatus
import com.amazonaws.services.kinesis.producer.{
  KinesisProducer,
  KinesisProducerConfiguration
}
import com.amazonaws.services.kinesis.{
  AmazonKinesis,
  AmazonKinesisClientBuilder
}
import example.dockerkit.{DockerDynamoService, DockerKinesisService}
import example.kinesis.Implicits._
import org.scalatest.{BeforeAndAfterAll, Suite}
import software.amazon.awssdk.regions.Region

trait KinesisTestKit extends BeforeAndAfterAll {
  self: Suite with DockerDynamoService with DockerKinesisService =>

  def streamName: String
  def recordProcessorFactory: IRecordProcessorFactory

  def shardCount = 4
  def applicationName = s"application-${UUID.randomUUID()}"
  def workerId = s"worker-${UUID.randomUUID()}"

  // Kinesis Client
  System.setProperty("com.amazonaws.sdk.disableCertChecking", "true")
  System.setProperty("com.amazonaws.sdk.disableCbor", "true")
  val kinesisClient: AmazonKinesis = AmazonKinesisClientBuilder.standard
    .withEndpointConfiguration(
      new EndpointConfiguration(
        s"https://localhost:$kinesisPort",
        Region.US_EAST_1.toString
      )
    )
    .build()

  // DynamoClient
  val dynamoClient: AmazonDynamoDB = AmazonDynamoDBClientBuilder
    .standard()
    .withEndpointConfiguration(
      new EndpointConfiguration(
        s"http://localhost:$dynamoPort",
        Region.US_EAST_1.toString
      )
    )
    .build()

  // KPL
  val kinesisProducer: KinesisProducer = new KinesisProducer(
    new KinesisProducerConfiguration()
      .setRegion(Region.US_EAST_1.toString)
      .setKinesisEndpoint("localhost")
      .setKinesisPort(kinesisPort)
      .setVerifyCertificate(false)
  )

  // KCL
  System.setProperty("aws.cborEnabled", "false")
  val kinesisConsumer: Worker = new Worker.Builder()
    .recordProcessorFactory(recordProcessorFactory)
    .config(
      new KinesisClientLibConfiguration(
        applicationName,
        streamName,
        new DefaultAWSCredentialsProviderChain(),
        workerId
      ).withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON)
        .withKinesisEndpoint(s"https://kinesalite:$kinesisPort")
        .withDynamoDBEndpoint(s"http://localhost:$dynamoPort")
    )
    .metricsFactory(new NullMetricsFactory())
    .build()

  val schedulerThread = new Thread(kinesisConsumer)
  schedulerThread.setDaemon(true)

  override def beforeAll(): Unit = {
    super.beforeAll()
    kinesisClient.createStream(streamName, shardCount)
    kinesisClient.waitUntil(streamName, StreamStatus.ACTIVE)
    schedulerThread.start()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    schedulerThread.interrupt()
  }

}
