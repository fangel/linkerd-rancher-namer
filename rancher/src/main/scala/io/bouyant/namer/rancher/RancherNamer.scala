package io.bouyant.namer.rancher

import com.twitter.finagle.buoyant.ExistentialStability._
import com.twitter.finagle._
import com.twitter.logging.Logger
import com.twitter.util.Activity.State
import com.twitter.util._

class RancherNamer(
  prefix: Path,
  refreshInterval: Duration,
  portMappings: Option[Map[String, Int]]
) (implicit val timer: Timer) extends Namer {
  private[this] val log = Logger.get("rancher")

  private[this] val ports = Map(
    "http" -> 80,
    "https" -> 443
  ) ++ (portMappings match  {
    case Some(additionalPorts) => additionalPorts
    case None => Map()
  })

  private[this] val client = new RancherClient(refreshInterval, log)

  private[this] def lookupPort(port:String):Option[Int] =
    (Try(port.toInt).toOption, ports.get(port)) match {
      case (Some(portNum), _) => Some(portNum)
      case (_, Some(portNum)) => Some(portNum)
      case _ => None
    }

  def lookup(path: Path): Activity[NameTree[Name]] = path.take(3) match {
    case phd@Path.Utf8(port, stack, service) => lookupPort(port) match {
      case Some(portNum) =>
        log.debug("port: %s, stack: %s, service: %s", portNum, stack, service)

        val containers:Activity[Option[Addr]] = client.activity.map { allContainers =>
          val eligable:Set[Address] = allContainers
            .filter(c => c.stackName == stack && c.serviceName == service)
            .collect(scala.Function.unlift(_.toAddr(portNum)))
            .toSet

          Option(eligable).filter(_.nonEmpty).map(Addr.Bound(_))
        }
        val stabilized:Activity[Option[Var[Addr]]] = containers.stabilizeExistence
        stabilized.map {
          case Some(addr) =>
            NameTree.Leaf(Name.Bound(addr, prefix ++ phd, path.drop(3)))
          case None =>
            NameTree.Neg
        }
      case _ =>
        log.warning("unable to understand port: %s", port)
        Activity.value(NameTree.Neg)
    }
    case _ => Activity.value(NameTree.Neg)
  }
}
