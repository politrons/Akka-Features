Author  Pablo Perez Garcia

![My image](src/main/resources/img/akka.png)


Most common features of Akka ecosystem of lightbend


* **Actors**

    ![My image](src/main/resources/img/akkaActor.png)
    * [Master] (src/main/scala/actor_system/actor/Master.scala)
    * [Worker] (src/main/scala/actor_system/actor/Worker.scala)
    * [Listener] (src/main/scala/actor_system/actor/Listener.scala)

    To run test execution [Here](src/main/scala/Runner.scala)

* **Persistence**

    ![My image](src/main/resources/img/event.png)
    * [Actor] (src/main/scala/persistence/actor/BasketActor.scala
    * [Commands] (src/main/scala/persistence/commands)
    * [Events] (src/main/scala/persistence/events)

    To run test execution [Here](src/main/scala/persistence/StreamRunner.scala)


* **Stream**

    ![My image](src/main/resources/img/stream.png)
    * [Operators] (src/main/scala/stream/AkkaStream.scala)
    * [Subscriber] (src/main/scala/stream/Subscriber.scala)
    * [Back-pressure] (src/main/scala/stream/BackPressure.scala)

* **Features**

    * [Agent] (src/main/scala/agents/features/Agents.scala)


