package example.consumerkcl1
import akka.actor.ActorRef
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{IRecordProcessor, IRecordProcessorFactory}
import com.amazonaws.services.kinesis.clientlibrary.types.{InitializationInput, ProcessRecordsInput, ShutdownInput}
import example.actors.counter.IncrementCmd
import example.kinesis.events.SomethingHasHappenedKinesisEvt

class RecordProcessorKCL1(actorRef: ActorRef) extends IRecordProcessor {
  override def initialize(initializationInput: InitializationInput): Unit =
    println(
      s"Initialising. ShardId = ${initializationInput.getShardId} | Sequence = ${initializationInput.getExtendedSequenceNumber}"
    )
  override def processRecords(
    processRecordsInput: ProcessRecordsInput
  ): Unit = {
    println(s"processing ${processRecordsInput.getRecords.size()} records")
    processRecordsInput.getRecords.forEach { r =>
      println(
        s"processing record. PK: ${r.getPartitionKey} | Seq: ${r.getSequenceNumber}"
      )

      val byteBuffer = r.getData
      val data = new Array[Byte](byteBuffer.remaining())
      byteBuffer.get(data)
      try {
        val kinesisEvent = SomethingHasHappenedKinesisEvt.parseFrom(data)
        println(s"Kinesis Message received: $kinesisEvent")
        actorRef ! IncrementCmd(kinesisEvent.value, kinesisEvent.user)
        // In reality we should checkpoint only after all records have been processed, so the Counter Actor should send an ACK,
        // and only after that we should checkpoint
        processRecordsInput
          .getCheckpointer()
          .checkpoint(r)
      } catch {
        case e =>
          println(
            s"ERROR: cannot parse protobuf\n\n${data.map(_.toInt).mkString(",")}"
          )
      }
    }
  }
  override def shutdown(shutdownInput: ShutdownInput): Unit =
    println("shutdown requested")
}
object RecordProcessorKCL1 {
  def factory(actorRef: ActorRef): IRecordProcessorFactory =
    () => new RecordProcessorKCL1(actorRef)
}
