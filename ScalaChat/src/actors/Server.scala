package actors

import collection.mutable.Map
import messages._
import messages.Unregister
import messages.StartUp
import messages.RegisteredClients
import messages.RegisterClientMessage
import messages.PrivateMessage
import messages.ChatMessage
import akka.actor._
import com.typesafe.config.ConfigFactory

object ChatServerApplication extends App {
  println("Avvio Server")
  val system = ActorSystem("AkkaChat", ConfigFactory.load.getConfig("chatserver"))
  val server = system.actorOf(Props[ChatServerActor], name = "chatserver")
  server ! StartUp
}

class ChatServerActor extends Actor {

  // Map dei client connessi
  val connectedClients:Map[String, ActorRef] = Map() //<-- mutable map

  def receive = {

    case m @ ChatMessage(x: String) =>
      println(sender.path.name + ": " + x)
      
      // questo messaggio inviato a tutti eccetto il mittente
      connectedClients.values.filter(_ != sender).foreach(_.forward(m))

    case RegisterClientMessage(client: ActorRef, identity: String) =>
        if(connectedClients.contains(identity)){
          println(s"${identity} ha tentato di unirsi da ${client}")
          sender ! ChatInfo(s"REGISTRAZIONE FALLITA: ${identity} già registrato")
        }else{
          println(s"${identity} si è aggiunto alla stanza da ${client.path.address.host}")
          
          // aggiunta del nuovo client alla map dei client connessi
          connectedClients += (identity -> client)
          sender ! ChatInfo("REGISTRATO")
        }

    case m @ PrivateMessage(target, _) =>
      connectedClients.values.filter(_.path.name.contains(target)).foreach(_.forward(m))

    case StartUp =>
      println("Ricevuto segnale di avvio")
      println(self)

    case RegisteredClients =>
      println(s"${sender.path.name} richiesta per la lista della stanza")
      sender ! RegisteredClientList(connectedClients.keys)

    case Unregister(identity) =>
        println(s"${identity} lascia questa stanza")
        
        // rimuove il client da quelli registrati
        connectedClients.remove(identity).foreach(_ ! PoisonPill)

    case _ => println("Comunicazione fuori protocollo")
  }
}