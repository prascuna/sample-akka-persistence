package example.dockerkit
import com.whisk.docker.impl.dockerjava.DockerKitDockerJava
import com.whisk.docker.{DockerContainer, DockerKit, DockerReadyChecker}

import scala.concurrent.duration._

trait DockerDynamoService extends DockerKit with DockerKitDockerJava {

  val dynamoPort = 8000

  private val dynamoContainer = DockerContainer("amazon/dynamodb-local")
    .withPorts(8000 -> Some(dynamoPort))
      .withReadyChecker(
        DockerReadyChecker.HttpResponseCode(port = 8000, code = 400).looped(attempts = 10, 250.millis)
      )

  abstract override def dockerContainers: List[DockerContainer] =
    dynamoContainer :: super.dockerContainers

  override def startAllOrFail(): Unit = {
    println(s"Starting Dynamo Docker Container at port $dynamoPort")
    super.startAllOrFail()
  }

  override def stopAllQuietly(): Unit = {
    println(s"Stopping Dynamo Docker Container at port $dynamoPort")
    super.stopAllQuietly()
  }
}
