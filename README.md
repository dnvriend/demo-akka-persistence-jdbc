# demo-akka-persistence-postgres
This is a small demo project on how to configure your application to use [akka-persistence-postgres](https://github.com/SwissBorg/akka-persistence-postgres).

It shows the following:
  - How to configure akka-persistence-postgres,
  - How to connect to a Postgres database,
  - The postgres database schema to use,
  - How to persist events to the journal,
  - How to create, configure and use Google Protocol Buffers for your data model,
  - How to create, configure and an EventAdapter, to tag events, and convert your domain model to your data model (protobuf),
  - How to create, configure and use Custom Serializers to convert your data model (protobuf) to byte arrays and vice-versa,
  - How to query for events using the akka-persistence-query interface,
  - How to query tagged events using the akka-persistence-query interface,
  - How to create your own custom persistency strategy using a custom journal dao using three tables, one `event-log` and two `event-content` tables for storing event data typed (that is query-able, I would not recommend this strategy though, but its possible!),
  - In short, how easy it is to create an event sourced application.
  
Have fun!
