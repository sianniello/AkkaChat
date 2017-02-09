package actors

import akka.actor._
import akka.remote.RemoteScope
import com.typesafe.config.ConfigFactory
import messages._
import java.net.{NetworkInterface, InetAddress}
import messages.RegisteredClientList
import messages.PrivateMessage
import messages.ChatMessage
import messages.ChatInfo
import scala.collection.convert.Wrappers.JEnumerationWrapper
import collection.JavaConversions._

object ChatClientApplication {

	def main(args:Array[String]) {  

		println("Avvio Chat Client")
		val interfaces = new JEnumerationWrapper(NetworkInterface.getNetworkInterfaces).toList.filter(!_.isLoopback).filter(_.isUp)
		
		val ipAddress = interfaces.head.getInterfaceAddresses.filter(_.getBroadcast != null).head.getAddress.getHostAddress 
		val identity = readLine("nickname: ")

		/* tinker with configurations so that our client uses it's own IP address and not one that is 
		 * hard-coded in application.conf. In short it this line was not there my ip would be 
		 * 127.0.0.1
		 */
		val clientConfig = ConfigFactory.parseString(s"""akka.remote.netty.tcp.hostname="$ipAddress" """)
		val defaultConfig = ConfigFactory.load.getConfig("chatclient")
		val completeConfig = clientConfig.withFallback(defaultConfig)

		/* construct system using the complete config which is a result of "merging"
		 * the parsed string and the default configs from the akka remote sub-system
		 */
		val system = ActorSystem("AkkaChat", completeConfig)

		/*
		 * binding con il server
		 */
		val serverconfig = ConfigFactory.load.getConfig("chatserver")
		val serverAddress = serverconfig.getString("akka.remote.netty.tcp.hostname")
		val serverPort = serverconfig.getString("akka.remote.netty.tcp.port")
		val serverPath = s"akka.tcp://AkkaChat@$serverAddress:$serverPort/user/chatserver"
		val server = system.actorSelection(serverPath) // <-- this is where we get the server reference
		println(server)
		// costruzione del client utilizzando l'ActorSystem definito
		val client = system.actorOf(Props(classOf[ChatClientActor]), name = identity)

		// espressione regolare per l'invio di un messaggio privato nel formato @nickname messaggio
		val privateMessageRegex = """^@([^\s]+) (.*)$""".r

		println("Digita /join per entrare in chat")
		println("Digita /list per la lista degli utenti connessi")
		println("Digita /leave per lasciare la chat")
		println("Digita @'nickname' per mandare un messaggio privato")

		// Ciclo infinito per l'invio dei messaggi

		Iterator.continually(readLine("> ")).takeWhile(_ != "/exit").foreach { msg =>
		msg match {
		case "/list" =>
		server.tell(RegisteredClients, client)

		case "/join" =>
		server.tell(RegisterClientMessage(client, identity), client)

		case "/leave" => 
		server.tell(Unregister(identity), client)

		case privateMessageRegex(target, msg) =>
		server.tell(PrivateMessage(target, msg), client)

		case _ =>
		server.tell(ChatMessage(msg), client)
		}
		}

		println("Uscita...")
		// Notifica il server di rimuovere il client da quelli registrati
		server.tell(Unregister(identity), client)
	}
}

class ChatClientActor  extends Actor {

	def receive = {

	case ChatMessage(message) =>
	println(s"${sender.path.name}: $message")

	case ChatInfo(msg) =>
	println ("INFO: ["+ msg +"]")

	case PrivateMessage(_, message) =>
	println(s"- ${sender.path.name}: $message")

	case RegisteredClientList(list) =>
	for (x <- list) println(x)

	case _ => println("Il client ha ricevuto qualcosa")
	}
}
