package clusterconsole

import akka.actor.{ Props, ActorRef, ActorSystem }
import akka.util.Timeout
import clusterconsole.core.LogF
import clusterconsole.http.{ ClusterAwareActor, RouterActor, HttpServiceActor, ClusterMemberUp }
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object ClusterConsoleApp extends App with LogF {

  args.logDebug("ClusterConsoleApp starting with args:" + _.toList.toString)

  val akkaConf =
    """akka.remote.netty.tcp.hostname="127.0.0.1"
      |akka.remote.netty.tcp.port=2771
      |akka.cluster.roles = [clusterconsole]
      |""".stripMargin

  val config = ConfigFactory.parseString(akkaConf).withFallback(ConfigFactory.load())

  val system = ActorSystem("ClusterConsoleSystem", config)

  val router: ActorRef = system.actorOf(Props[RouterActor], "router")
  val clusterAwareActor: ActorRef = system.actorOf(Props(classOf[ClusterAwareActor], router))

  system.scheduler.schedule(3 seconds, 10 seconds, clusterAwareActor, ClusterMemberUp("cluster1", "name: " + System.currentTimeMillis()))

  system.actorOf(HttpServiceActor.props("127.0.0.1", 8080, Timeout(30 seconds), router, clusterAwareActor), "clusterconsolehttp")

}