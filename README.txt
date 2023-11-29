### Setting Up:

Firstly, run the server.
To select a server port when using command line, execute the program with the following arguments:
-csp [port number]

You can now run client programs and connect with the server.
To select a port to connect with when using command line, execute the program with the following arguments:
-ccp [port number]
To select an address to connect to when using command line, execute the program with the following arguments:
-cca [port number]

### ChatClient:

ChatClient allows you to communicate with other clients on the server.
You must firstly enter a username which will uniquely identify you as a client.
You can enter text into the console, which is then sent to all other clients on the server.
As a ChatClient you can also read messages from other clients on the server.

### Whispering:

To send a message to a specific client on the server, you must precede your message with '@username', where 'username' is the username of the client you want to send to.
A normal received message will appear as the following:
[sender] message
A message which has been whispered to you will appear as the following:
(sender) message

### ChatBot:

To talk to a chat bot, you can do one of the following:
- whisper a message to the chat bot
- precede your message with 'chatbot' (in any combination of upper and lower case)

The chat bot will respond to certain messages you send to it.
If you whisper a message to a chat bot, it will whisper back.

Messages which ChatBot responds to:
- hello
- hi
- hey
- hello there
- how are you?
- tell me a joke
- when were you born?
- tell me a dog fact
- go away

### DoDClient:

Having a DoDClient in the chat allows chat clients to play Dungeons of Doom together.
To be able to play, there must be a map file in the same folder as DoD, with the name 'map.txt'.

The DoDClient runs one continuous game of DoD.
Players can join the game by typing 'JOIN' into chat.
They can then type in any DoD command into chat to perform an action within the game.
The result of this command will be whispered back, so only you can see it.

If you leave on an exit space with enough gold, you win the game.
If you're caught by the bot or try to leave when you can't, you lose the game.
You automatically leave the game once you win or lose, but you can join back to try again.

When new players join, gold is automatically added to the map at random, to make sure it is possible for the new player to win.

### Exiting Programs:

To leave the chat and exit the program as a client (of any type), type 'LEAVE' into the client console.
To shut down the server, disconnect clients, and exit all server and client programs, type 'EXIT' into the server console.