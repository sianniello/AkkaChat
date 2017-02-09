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
		/* construct client with current machine's IP address instead of using the config value
		 * get network interfaces make sure it's not loopbak (i.e. points outside of itself)
		 * and filter for those that are actually "up"
		 */
		val interfaces = new JEnumerationWrapper(NetworkInterface.getNetworkInterfaces).toList.filter(!_.isLoopback).filter(_.isUp)
		/** Ideally this should give a list of ip addresses and then we choose the one we want
		 * but alas I am lazy so just pop the first ip address that works and use it instead
		 * I use getBroadcast here as a subtle way of filtering out IPV6 addresses they have 
		 * a null value
		 */
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

		// costruzione del client utilizzando l'ActorSystem definito
		val client = system.actorOf(Props(classOf[ChatClientActor]), name = identity)

		// some input parsing logic to filter out private messages and so special things to it
		// like NOT Broadcast it to all connected clients
		val privateMessageRegex = """^@([^\s]+) (.*)$""".r

		// TODO implementare la funzione /help 
		println("Digita /join per entrare in chat")

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
