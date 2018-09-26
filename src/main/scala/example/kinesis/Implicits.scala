package example.kinesis
import java.nio.ByteBuffer

import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.model.StreamStatus
import com.amazonaws.services.kinesis.producer.{
  KinesisProducer,
  UserRecordResult
}
import com.google.common.util.concurrent.{
  FutureCallback,
  Futures,
  ListenableFuture
}

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}

object Implicits {
  implicit class AmazonKinesisOps(client: AmazonKinesis) {
    def waitUntil(streamName: String, status: StreamStatus): Unit = {
      val backoff: Duration = 250.millis
      while (client
               .describeStream(streamName)
               .getStreamDescription
               .getStreamStatus != status.toString) {
        Thread.sleep(backoff.toMillis)
      }
    }
  }

  implicit final class KinesisProducerOps(producer: KinesisProducer) {
    def addUserRecord(stream: String,
                      partitionKey: String,
                      data: Array[Byte]): Future[UserRecordResult] = {
      val promise = Promise[UserRecordResult]()
      val callback = new FutureCallback[UserRecordResult] {
        override def onSuccess(result: UserRecordResult): Unit =
          promise.success(result)
        override def onFailure(t: Throwable): Unit = promise.failure(t)
      }
      val listenableFuture: ListenableFuture[UserRecordResult] =
        producer.addUserRecord(stream, partitionKey, ByteBuffer.wrap(data))

      Futures.addCallback(listenableFuture, callback)
      promise.future
    }
  }

}
