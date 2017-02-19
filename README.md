# ScalaChat

##Intro
La chat è ispirata al protocollo IRC e permette la comunicazione sia all'interno di stanze in cui tutti leggono tutto, sia
privata tra due utenti. Per l'implementazione oltre alla libreria nativa di scala sono stati utilizzati gli __attori__ importanto
la libreria __akka__.

Il progetto è composto da due package: _messages_ e _actors_. Il package messages contiene un trait sailed che specifica
tutti i possibili tipi di messaggi che possono scambiarsi gli attori.
Il package actors contiene le due classi principali che consensono la comunicazione: _Client_ e _Server_.


##Server
Il server utilizza un __mutable set__ per memorizzare l'insieme dei client connessi. La concorrenza è garantita dal fatto che lo stato
dell'attore non è in condivisione con nessun altro thread e ogni messaggio ricevuto è gestito da akka in maniera sequenziale.

La funzione di ricezione basa il proprio comportamento sul __pattern matching__ dei messaggi ricevuti (definiti nel trait Message). 
Attraverso le funzioni _filter_ e _foreach_ è possibile trovare il destinatario di un messaggio privato ed escludere il mittente dal 
broadcast. La comunicazione privata tra i client è trasparente per gli utenti, questi ultimi potranno scambiarsi messaggi senza rendersi
conto della presenza del server.

##Client
Ha il compito di interagire con l'utente e di gestire il collegamento con il server. La parte principale risiede all'interno del ciclo 
__Iterator.continually_ in cui il client invia i comandi creati dall'utente e resta in attesa fino al comando successivo.
Nella funzione __tell_ specificando come secondo parametro il client stesso è possibile far ricevere le risposte direttamente all'attore
mittente coinvolto.

##Configurazione
Sono presenti due file di configurazione: _application_ e _common_ utilizzati fondamentalmente per il settaggio degli indirizzi ip e di porta
di client e server.

##Esecuzione
Per l'esecuzione basta avviare prima il server e successivamente il client. Il client richiederà di inserire un nickname e stamperà
la lista dei comandi disponibili.
Per connettersi al server principale basta solo digitare il comando _/join_.

##Librerie utilizzate
- standard JRE 1.8.0_121
- akka 2.11
- scala 2.11.8
