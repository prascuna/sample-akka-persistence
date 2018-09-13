package example.consumer.kinesis
import java.nio.ByteBuffer

import example.ConsumerApp.counterActor
import example.actors.counter.IncrementCmd
import example.kinesis.events.SomethingHasHappenedKinesisEvt
import software.amazon.kinesis.lifecycle.events._
import software.amazon.kinesis.processor.{ShardRecordProcessor, ShardRecordProcessorFactory}

class SampleShardRecordProcessor extends ShardRecordProcessor {
  override def initialize(initializationInput: InitializationInput): Unit =
    println(s"Initialising. ShardId = ${initializationInput
      .shardId()} | Sequence = ${initializationInput.extendedSequenceNumber()}")

  override def processRecords(
    processRecordsInput: ProcessRecordsInput
  ): Unit = {

    println(s"processing ${processRecordsInput.records().size()} records")
    processRecordsInput.records().forEach { r =>
      println(
        s"processing record. PK: ${r.partitionKey()} | Seq: ${r.sequenceNumber()}"
      )
      val byteBuffer: ByteBuffer = r.data()
      val data = new Array[Byte](byteBuffer.remaining())
      byteBuffer.get(data)
      try {
        val kinesisEvent = SomethingHasHappenedKinesisEvt.parseFrom(data)
        println(s"Kinesis Message received: $kinesisEvent")
        counterActor ! IncrementCmd(kinesisEvent.value, kinesisEvent.user)
        // In reality we should checkpoint only after all records have been processed, so the Counter Actor should send an ACK,
        // and only after that we should checkpoint
        processRecordsInput
          .checkpointer()
          .checkpoint(r.sequenceNumber(), r.subSequenceNumber())
      } catch {
        case e =>
          println(
            s"ERROR: cannot parse protobuf\n\n${data.map(_.toInt).mkString(",")}"
          )
      }
    }
  }
  override def leaseLost(leaseLostInput: LeaseLostInput): Unit =
    println(
      "Lease lost. Not sure what to do here. We should probably terminate"
    )

  override def shardEnded(shardEndedInput: ShardEndedInput): Unit = {
    println("Shard ended")
    shardEndedInput.checkpointer().checkpoint()

  }
  override def shutdownRequested(
    shutdownRequestedInput: ShutdownRequestedInput
  ): Unit = {
    println("shutdown requested")
    //      shutdownRequestedInput.checkpointer().checkpoint()
  }
}
object SampleShardRecordProcessor {
  val factory = new ShardRecordProcessorFactory {
    override def shardRecordProcessor(): ShardRecordProcessor =
      new SampleShardRecordProcessor()
  }
}
