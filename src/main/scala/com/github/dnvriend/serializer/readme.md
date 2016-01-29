# Package: com.github.dnvriend.serializer
Place your `akka.serializer.Serializer`(s) here. They will be used by akka-persistence to serialize
messages to an Array[Byte], just before they are persisted to the data store. The serializer will also be used to 
serialize messages when sending a message between actors.

The idea is to split your model into a domain model and a data model. The domain model uses a vocabulary
optimized for the domain. The data model is useful for persisting messages, for remoting and the vocabulary is 
optimized for transmitting and/or storing data. 

The domain model usually uses case classes to communicate. When the domains models are remote, like eg. with 
akka-remoting or akka-cluster, then encoding the messages to eg. Google Protobuf messages is the way to go. Because protobuf 
supports schema evolution it is also very useful for persisting messages and reading them later, maybe years later. 