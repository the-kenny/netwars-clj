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
When the client connects, it sends the server a message with type :helo and optional metadata. As a response, the server sends the following messages to set up the client:

- A message of type `:game-list`
- A message of type `:unit-tiles`

### Creating a new Game
- Client requests: `:new-game`
- Server responds: `:new-game`
- Server sends additional data: 
	- `:game-data`
	- `:map-data`
	- `:unit-data`
- Server sends `:new-listed-game` to all clients

### Joining a running Game


### In-Game Messages


## Message Types

### :game-list
Contains a map with all running games. game-id as the key, general game-info as the value:

    {:type :game-list,
     :games {"cd8e8f28-522d-48db-87a8-bde63b9372f1" 
               {:map-name "7330.aws", ...}}}

### :unit-tiles
Contains a tile for the unit images. The spec is generated from a folder structure and maps a list of directory-names (for example: `[:os :lander]`) to a coordinate in the tile-image. The spec is is accessible via the key :tile-spec, the tiled image is accessible via `:tiled-image`.

    {:type :unit-spec
     :tile-spec {[:os :lander] [100, 0],
                 [:bh :infantry] [120, 0]}
     :tiled-image "data:image/png…"}

### :new-game
The client sends a message with type `:new-game` to the server. The only other mandatory key is `:map-name` which is a string of the map-file.

    {:type :new-game,
     :map-name "7330.aws"}

The server will create a new game, set everything up and will send a response to the client.

    {:type :new-game,
     :game-id "c2439a37-a479-4b69-a740-d3f8f23405d8"}

`:game-id` is used to identity the game on the server.

### :new-listed-game
A broadcast notifying connected clients of a new game. 

    {:type :new-listed-game,
     :game {:info {…},
            :game-id "c2439a37-a479-4b69-a740-d3f8f23405d8"}}