package io.bouyant.namer.rancher

import com.twitter.finagle.buoyant.ExistentialStability._
import com.twitter.finagle._
import com.twitter.logging.Logger
import com.twitter.util.Activity.State
import com.twitter.util._

class RancherNamer(
  prefix: Path,
  refreshInterval: Duration
) (implicit val timer: Timer) extends Namer {
  private[this] val log = Logger.get("rancher")

  private[this] val client = new RancherClient(refreshInterval, log)

  def lookup(path: Path): Activity[NameTree[Name]] = path.take(2) match {
    case phd@Path.Utf8(stack, service) =>
      log.debug("stack: %s, service: %s", stack, service)

      val containers:Activity[Option[Set[RancherContainer]]] = client.activity.map { allContainers =>
        val eligable:Set[RancherContainer] = allContainers.filter(
          (c:RancherContainer) => c.stackName == stack && c.serviceName == service
        ).toSet

        eligable.size match {
          case 0 => None
          case _ => Some(eligable)
        }
      }
      val stabilized:Activity[Option[Var[Set[RancherContainer]]]] = containers.stabilizeExistence
      val tree:Activity[NameTree[Name]] = stabilized.map {
        case Some(eligable) =>
          val addr:Var[Addr] = eligable.map(_.map(_.toAddr)).map(Addr.Bound(_))
          NameTree.Leaf(Name.Bound(addr, prefix ++ phd, path.drop(2)))
        case None =>
          NameTree.Neg
      }
      tree
    case _ => Activity.value(NameTree.Neg)
  }
}
