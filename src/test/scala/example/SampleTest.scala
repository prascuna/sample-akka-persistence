package example

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{
  IRecordProcessor,
  IRecordProcessorFactory
}
import com.amazonaws.services.kinesis.clientlibrary.types.{
  InitializationInput,
  ProcessRecordsInput,
  ShutdownInput
}
import com.whisk.docker.scalatest.DockerTestKit
import example.dockerkit.{DockerDynamoService, DockerKinesisService}
import example.kinesis.Implicits._
import org.scalatest._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class SampleTest
    extends TestKit(ActorSystem("SampleTest"))
    with ImplicitSender
    with FunSpecLike
    with DockerTestKit
    with DockerDynamoService
    with DockerKinesisService
    with KinesisTestKit
    with Matchers {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  val echoActor = system.actorOf(TestActors.echoActorProps)

  override lazy val streamName: String = "test-stream"
  override lazy val recordProcessorFactory: IRecordProcessorFactory = () =>
    new IRecordProcessor {
      override def initialize(input: InitializationInput): Unit = ()
      override def processRecords(input: ProcessRecordsInput): Unit =
        input.getRecords.forEach { r =>
          val byteBuffer = r.getData
          val data = new Array[Byte](byteBuffer.remaining())
          byteBuffer.get(data)
          echoActor ! data
        }
      override def shutdown(input: ShutdownInput): Unit = ()
  }

  describe("When a producer writes into a stream") {

    it("a consumer should be able to read from it") {
      val bytes = s"test-data-${Math.random()}".getBytes

      kinesisProducer.addUserRecord(
        streamName,
        "test-partition-" + Math.random(),
        bytes
      )
      expectMsgPF[Assertion](max = 2.minute) {
        case x: Array[Byte] if x.sameElements("asdf".getBytes) => succeed
        case x                                       => fail(s"Expected [${bytes.mkString(",")}], got [$x] instead")
      }
    }
  }
}
