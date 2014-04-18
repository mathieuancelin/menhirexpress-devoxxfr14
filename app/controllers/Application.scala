package controllers

import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.concurrent._

import play.api.libs.EventSource
import play.api.libs.ws.WS.WSRequestHolder
import play.api.libs.ws.WS

import java.util.concurrent.TimeUnit
import scala.util.Random

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

trait Event

case class Operation(amount: Int, level: String, userid: String, from: String, timestamp: Long) extends Event
case class OperationStatus(message: String, from: String, timestamp: Long) extends Event

object Application extends Controller {

  val operationFmt = Json.format[Operation]
  val statusFmt = Json.format[OperationStatus]

  val eventFmt = new Format[Event] {
    def reads(json: JsValue): JsResult[Event] = json \ "amount" match {
      case JsUndefined() => statusFmt.reads(json)
      case _ => operationFmt.reads(json)
    }

    def writes(o: Event): JsValue = o match {
      case st: OperationStatus => statusFmt.writes(st)
      case op: Operation => operationFmt.writes(op)
    }
  }

  def index(role: String) = Action {
    Ok(views.html.index(role))
  }

  def mobile() = Action {
    Ok(views.html.mobile())
  }

  def getStream(request: WSRequestHolder): Enumerator[Array[Byte]] = {
    val (iteratee, enumerator) = Concurrent.joined[Array[Byte]]
    request.get(_ => iteratee).map(_.run)
    enumerator
  }

  def feed(role: String, lower: Int, higher: Int) = Action {

    val lutece = getStream(WS.url("http://localhost:9000/operations?from=Lutece").withRequestTimeout(-1))
    val burdigala = getStream(WS.url("http://localhost:9000/operations?from=Burdigala").withRequestTimeout(-1))
    val lugdunum = getStream(WS.url("http://localhost:9000/operations?from=Lugdunum").withRequestTimeout(-1))
    val condevicnum = getStream(WS.url("http://localhost:9000/operations?from=Condevicnum").withRequestTimeout(-1))

    val inBounds: Enumeratee[Event, Event] = Enumeratee.collect[Event] {
      case st: OperationStatus => st
      case op@Operation(amount, _, _, _, _) if lower < amount && amount < higher => op
    }

    val secure: Enumeratee[Event, Event] = Enumeratee.collect[Event] {
      case st: OperationStatus if role == "MANAGER" => st
      case op@Operation(_, "public", _, _, _) => op
      case op@Operation(_, "private", _, _, _) if role == "MANAGER" => op
    }

    val pipeline = lutece >- burdigala >- condevicnum >- lugdunum

    val transformer = Enumeratee.map[Array[Byte]](Json.parse) ><>
      Enumeratee.map[JsValue](eventFmt.reads) ><>
      Enumeratee.collect[JsResult[Event]] { case JsSuccess(evt, _) => evt } ><>
      secure ><>
      inBounds ><>
      Enumeratee.map[Event](eventFmt.writes)

    Ok.feed( pipeline.through( transformer ).through( EventSource() ) ).as("text/event-stream")
  }

}