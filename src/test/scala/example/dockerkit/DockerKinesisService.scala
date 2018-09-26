package example.dockerkit
import com.whisk.docker.DockerReadyChecker.LogLineContains
import com.whisk.docker.impl.dockerjava.DockerKitDockerJava
import com.whisk.docker.{DockerContainer, DockerKit}

trait DockerKinesisService extends DockerKit with DockerKitDockerJava {

  val kinesisPort = 4567

  private val kinesisContainer = DockerContainer("instructure/kinesalite")
    .withPorts(4567 -> Some(kinesisPort))
    .withCommand("--ssl")
    .withReadyChecker(LogLineContains("Listening at https://:::4567"))

  abstract override def dockerContainers: List[DockerContainer] =
    kinesisContainer :: super.dockerContainers

  override def startAllOrFail(): Unit = {
    println(s"Starting Kinesalite Docker Container at port $kinesisPort")
    super.startAllOrFail()
  }

  override def stopAllQuietly(): Unit = {
    println(s"Stopping Kinesalite Docker Container at port $kinesisPort")
    super.stopAllQuietly()
  }
}
