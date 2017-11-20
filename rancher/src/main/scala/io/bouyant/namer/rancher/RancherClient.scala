package io.bouyant.namer.rancher

import com.fasterxml.jackson.databind.{ObjectMapper, PropertyNamingStrategy}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.annotation.JsonIgnoreProperties


import com.twitter.finagle.{Service, Http, Address}
import com.twitter.finagle.http
import com.twitter.finagle.service.Retries
import com.twitter.finagle.tracing.NullTracer

import com.twitter.logging.Logger
import com.twitter.util.Activity.State
import com.twitter.util.{Timer, Var, Activity, Duration, Future, Closable}

import java.util.concurrent.atomic.AtomicBoolean

case class PortMapping(
  val ip: String,
  val publicPort: Int,
  val privatePort: Int,
  val protocol: String
) {
  // This is a bit of a hack - and should have more error-checking. But it
  // makes the JSON parsing work.
  // If anyone knows how to do a better constructor - or JSON Deserializer,
  // that would be nice
  def this(mapping: String) = this(
    mapping.split(":")(0),
    mapping.split(":")(1).toInt,
    mapping.split(":")(2).split("/")(0).toInt,
    mapping.split(":")(2).split("/")(1)
  )
  override def toString():String = s"$ip:$privatePort:$publicPort/$protocol"
}

@JsonIgnoreProperties(ignoreUnknown = true)
case class RancherContainer(
  createIndex: Int,
  dns: List[String],
  ips: List[String],
  primaryIp: String,
  labels: Map[String, String],
  name: String,
  ports: List[PortMapping],
  serviceName: String,
  stackName: String,
  state: String
) {
  def toAddr(port: Int):Option[Address] = state match {
    case "running" => Some(Address(primaryIp, port))
    case _ => None
  }
}

class RancherClient(
  refreshInterval: Duration,
  log: Logger
)(implicit val timer: Timer) {
  private[this] val objectMapper = new ObjectMapper() with ScalaObjectMapper
  objectMapper.registerModule(DefaultScalaModule)
  objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

  private[this] val service = Http.client
      .withStack(Http.client.stack.remove(Retries.Role))
      .withParams(Http.client.params) // ++ params
      .withLabel("io.buyant.namer.rancher.client")
      .withTracer(NullTracer)
      .newService(s"/$$/inet/rancher-metadata/80")

  private[this] val containers =
    Activity(Var.async[State[List[RancherContainer]]](Activity.Pending) { state =>
      val done = new AtomicBoolean(false)
      val initialized = new AtomicBoolean(false)

      Future.whileDo(!done.get) {
        val request = http.Request(http.Method.Get, "/2015-12-19/containers")
        request.host = "rancher-metadata"
        request.accept = "application/json"
        val response = service(request)
        response
          .map { _.getContentString() }
          .map { objectMapper.readValue[List[RancherContainer]] }
          .onSuccess {(resp:List[RancherContainer]) =>
            initialized.set(true)
            log.debug("fetched info about %s containers from Rancher", resp.length)
            state.update(Activity.Ok(resp))
          }
          .onFailure { e =>
            log.error("failed to fetch metadata. %s", e)
            if (!initialized.get) {
              state.update(Activity.Failed(e))
            }
          }

        // http://rancher-metadata/2015-12-19/version?wait=true&value=1294311-4f3c5c96fb170da7fa8781d4ac55192c&maxWait=10
        Future.sleep(refreshInterval)
      }

      Closable.make { _ =>
        log.debug("closing")
        done.set(true)
        Future.Unit
      }
    })

  def activity:Activity[List[RancherContainer]] = containers
}
