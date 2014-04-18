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
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.QueryOpts

object OperationWS extends Controller {

  def collection = ReactiveMongoPlugin.db.collection[JSONCollection]("sales")

  def websocket = WebSocket.using[JsValue] { _ =>
    val out = Enumerator.empty[JsValue]
    val in = Iteratee.foreach[JsValue](doc => collection.insert(doc))
    (in, out)
  }

  def operations(from: String) = Action {

    val enumerator = collection.find(Json.obj("from" -> from)).options(QueryOpts().tailable.awaitData).cursor[JsValue].enumerate()

    val noise = Enumerator.generateM[JsValue] {
      Promise.timeout(Some(
        Json.obj(
          "timestamp" -> System.currentTimeMillis(),
          "from" -> from,
          "message" -> "System ERROR"
        )
      ), 2000 + Random.nextInt(2000), TimeUnit.MILLISECONDS)
    }

    Ok.chunked( enumerator >- noise )
  }
}
