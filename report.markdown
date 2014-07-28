% Peer to Peer File search 
% Swair Shah, Miren Tanna
% 22-July-2014

# System Overview

## Message Types
A node can send/receive the following types of messages:

1. join 
2. fetch 
3. search
4. search_result
5. bye
6. bye_ack
7. can_i_leave
8. yes_you_can

### Join
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
Leave protocol is initiated by a node by sending a `can_i_leave` message
to its neighbours and sets a flag `_am_i_leaving` to `true`,
and waits for `yes_you_can` replies for 1 second. 

Until it received the replies from all its __current__ neighbours,
it keeps sending `can_i_leave` messages to its neighbours.

When the node receives `yes_you_can` replies from a neighbour,
it adds the sender of that message to `_leave_acks` list. The node
no longer sends `can_i_leave` to the neighbours in `_leave_acks`

As soon as there are no extra neighbours in `_node_lookup` as
compared to `_leave_acks`, the node randomly chooses one of its
neighbours and sends the list of its neighbours to it.
and now the node uses `bye` protocol to depart from the cluster
gracefully.

When a node received `can_i_leave` message, if it has its
`_am_i_leaving` flag set to `true` then it checks the corresponding 
node ids. if it has a lower id (higher priority) it dosesn't send
`yes_you_can` reply (Instead sends `no_you_cannot`, which is 
implemented just for debugging purposes and plays no role
in the protocol execution). 

If it has a lower priority then it sends a `yes_you_can` reply
to the neighbours.

Implementation is as follows:

```java
while(!ready_to_leave()) {
   /*
   send message to all neighbours except
   the ones in _leave_acks
    */
   for(String n : _connector._node_lookup.keySet()) {
       if (!_leave_acks.contains(n)) {
          _connector.send_message(leave_msg, new NodeInfo(n));
       }
   }
   try {
       Thread.sleep(1000);
   } catch (InterruptedException ex) {
       ex.printStackTrace();
   }
}

 _am_i_leaving = false;
 _leave_acks.clear();
```

```java
public synchronized boolean ready_to_leave() {
    ConcurrentHashMap<String,Integer> neighbours = 
        new ConcurrentHashMap<>(_connector._node_lookup);
    Set<String> pending = neighbours.keySet();

    System.out.println(_leave_acks);
    for(String n : pending) {
        if(_leave_acks.contains(n)) {
           pending.remove(n);
        }
    }
    return pending.isEmpty();
}
```

This protocol avoids deadlocks by breaking ties based on `node_id`. This also makes sure
that concurrent leave works fine. 

Say `n0` is connected to `n2` and `n3` and `n1`. `n1` is conneted to `n4` and `n5` also.
If `n0` and `n1` start Leave protocol concurrently, both have set their
`_am_i_leaving` to true. 

When `n0` receives `can_i_leave` from `n1`, it won't send a `yes_you_can`,
and `n1` in turn will keep looping in `ready_to_leave()` method.

But `n1` meanwhile __will__ send `yes_you_can` to `n0` considering it 
has a higher `node_id` and a lower priority. When `n0` gets `yes_you_can`
from all its neighbours, it picks one of its neighbours and makes it join
all other neighbours of `n0`. So this potentially changes `_node_lookup` 
of `n1` and in next `while(!ready_to_leave())` execution `n1` will send
`can_i_leave` to its updated neighbourhood (except from the nodes it
already received acks). and when it finally gets permission from all
its neighbours (old and new), it can leave.

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

### Search
The implementation of search procedure in Node class has
two important parts, one is the `SearchAgent` and other is 
`SearchKeeper`.

A `Search` message contains 
* message type `search`, 
* `NodeInfo` of sender, receiver, initiator
* current hopcount for the query 
* UUID of the search (which the initiator sets)

### Search_Result


