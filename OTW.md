# Netwars OTW Protocol

## Encoding
Netwars utilizes Websockets as the transport layer. Messages are strings containing clojure maps encoded with `pr-str`:

    {:type :ping}

Keys are keywords, values can be everything which is supported by pr-str and read-string.

### Message Types
Every message *must* contain the key :type with a keyword as value. This tuple is used to dispatch the message on the client/server.

### Coordinates
Coordinates are encoded as tuples of numbers:

    [1 2]

They can be marked with special metadata describing their type: 

    ^{:otw-type :Coordinate} [1 2]

### Images
Images get encoded as base64 and are prefixed to be ready to get used in the src-attribute of a HTML <img>-tag. Optionally, the server *can* send URIs instead of base64 encoded data. 


## Protocol Description
### Connecting

When the client connects, the server sends the following messages to set up the client:

- A message of type ```:game-list```
- A message of type ```:unit-tiles```

### Creating a new Game


### Joining a running Game



## Message Types

### :game-list
### :unit-tiles