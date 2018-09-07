package example
import java.nio.ByteBuffer
import java.time.LocalDateTime

import com.amazonaws.services.kinesis.producer.{KinesisProducer, KinesisProducerConfiguration, UserRecordResult}
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}
import example.kinesis.events.SomethingHasHappenedKinesisEvt
import software.amazon.awssdk.regions.Region

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}

object ProducerApp extends App {
  val config = new KinesisProducerConfiguration()
    .setRegion(Region.US_EAST_1.toString)
//    .setKinesisEndpoint("localhost")
//    .setKinesisPort(4567)

  val kinesisProducer = new KinesisProducer(config)

  val futures: immutable.Seq[Future[UserRecordResult]] = for {
    i <- 1 to 10
    cmd = SomethingHasHappenedKinesisEvt(1, s"user-$i", LocalDateTime.now().toString)
  } yield
    addUserRecord("team55-test-stream", s"partition-key-$i", cmd.toByteArray)

  val f: Future[immutable.Seq[String]] = Future.sequence(futures).map {
    _.map { res =>
      s"Result: shard: ${res.getShardId} | seq: ${res.getSequenceNumber} | successful: ${res.isSuccessful}"
    }
  }

  val res = Await.result(f, Duration.Inf)

  res.foreach(println)

  private def addUserRecord(stream: String,
                            partitionKey: String,
                            data: Array[Byte]): Future[UserRecordResult] = {
    val promise = Promise[UserRecordResult]()
    val callback = new FutureCallback[UserRecordResult] {
      override def onSuccess(result: UserRecordResult): Unit =
        promise.success(result)
      override def onFailure(t: Throwable): Unit = promise.failure(t)
    }
    val listenableFuture: ListenableFuture[UserRecordResult] =
      kinesisProducer.addUserRecord(stream, partitionKey, ByteBuffer.wrap(data))

    Futures.addCallback(listenableFuture, callback)
    promise.future

  }
}
