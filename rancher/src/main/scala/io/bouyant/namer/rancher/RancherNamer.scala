package io.bouyant.namer.rancher

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

      client.activity.map(allContainers => {
        val addresses = allContainers.filter(
          (c:RancherContainer) => c.stackName == stack && c.serviceName == service
        ).map(
          (c:RancherContainer) => {
            Address(c.primaryIp, c.ports(0).publicPort)
          }
        ).toSet

        log.debug("stack: %s, service: %s, result: %s", stack, service, addresses)

        NameTree.Leaf(
          Name.Bound(
            Var.value(Addr.Bound(addresses)),
            prefix ++ phd,
            path.drop(2)
          )
        )
      })
    case _ => Activity.value(NameTree.Neg)
  }
}
