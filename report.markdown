% Peer to Peer File search 
% Swair Shah, Miren Tanna
% 22-July-2014

## System Overview

### Message Types
A node can send/receive the following types of messages:

1. Join 
2. Leave
3. Search
4. Search_Result
5. Fetch 

#### Join
A `Join` message contains message type `join`, sender and receiver
`NodeInfo`. 

This message is sent when a node wishes to join a cluster of another 
node. Since the channels are reliable (TCP), there is no acknowledgment
of a message and sender assumes that message is going to be delivered.

The sender, N1 sends a `Join` message to N2. Then N1 adds N2 to its
`_neighbours` list, and establishes a TCP connection using its
`Connector`.

The message on the receiving end (N2) of this a `Join` message in turn
adds N1 to its `_neighbours` list and establishes a TCP connection to it.

### Leave
#### Bye protocol

`Bye protocol` deals with closing of listener threads.
It is the last thing done as a part of Leave protocol.
It is handled in listener threads, and not passed on to 
`Connector` or `Node` class.

```
 if (message type is bye)
    send(bye_ack message to sender)
    terminate the current listener thread
    remove (sender from _node_loookup)

 if (message type is bye_ack)
    terminate the current listener thread
    remove (sender from _node_loookup)
```

#### Search
A `Search` message contains 
* message type `search`, 
* `NodeInfo` of sender, receiver, initiator
* current hopcount for the query 
* UUID of the search (which the initiator sets)

#### Search_Result


