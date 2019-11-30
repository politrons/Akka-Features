![My image](../src/main/resources/img/cloudstate.png)
 
# Rest connector Service

Service Connectors make services easier to use by hiding the specifics of communications-related APIs. Also as a k8s service it does not
require any reboot or update the service that consume this connector, as long as the contract between them does not change.

As a service is easier to scale up and down, if it's required at some point.

### ![My image](../src/main/resources/img/grpc.png) ![My image](../src/main/resources/img/finagle.png)

This **gRPC connector as service** it's implemented using the current [Cloudstate DSL](https://cloudstate.io/docs/user/features/index.html).
 and also resilient [Finagle](https://twitter.github.io/finagle/guide/index.html) client
 
Here I implement an CRDT(Conflict free replicate data type) connector service.

* [Protobuf files](src/main/proto)

* [Rest connector](src/main/scala/io.cloudstate.connector/RestConnector.scala)

### ![My image](../src/main/resources/img/docker.png)

**Cloudstate** deploy StatefulService using docker images, so the service that you implement must be save into a docker image
that run that service.

* Plugin to Create and push image of the Service into docker hub

```

        <plugin>
                <groupId>io.fabric8</groupId>
                <artifactId>docker-maven-plugin</artifactId>
                <version>0.26.1</version>
                <configuration>
                    <images>
                        <image>
                            <alias>service</alias>
                            <name>politrons/rest-connector:%l</name>
                            <build>
                                <from>java:8</from>
                                <tags>
                                    <tag>latest</tag>
                                </tags>
                                <assembly>
                                    <descriptorRef>artifact-with-dependencies</descriptorRef>
                                </assembly>
                                <entryPoint>
                                    <arg>java</arg>
                                    <arg>-cp</arg>
                                    <arg>/maven/*</arg>
                                    <arg>io.cloudstate.connector.RestConnectorMain</arg>
                                </entryPoint>
                            </build>
                        </image>
                    </images>
                </configuration>
                <executions>
                    <execution>
                        <id>build-docker-image</id>
                        <phase>package</phase>
                        <goals>
                            <goal>build</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>    
```

Using in the plugin ```<name>politrons/rest-connector:%l</name>``` it means it will be publish in my personal docker hub **politrons**

* Create image ``Docker build``
* Push image ``Docker push``


### ![My image](../src/main/resources/img/kubernete.png)

* Create a **service** yaml file, and add the ``StatefulStore``, ``StatefulService`` and the ``Service`` to route connections from outside the cluster to the ``StatefulService``

    ```
    apiVersion: cloudstate.io/v1alpha1
    kind: StatefulService
    metadata:
      name: cloudstate-rest-connector
      labels:
        app: cloudstate-rest-connector
    spec:
      containers:
        - image: politrons/rest-connector:latest
    ---
    
    apiVersion: v1
    kind: Service
    metadata:
      name: cloudstate-rest-connector-service
    spec:
      type: LoadBalancer
      ports:
        - port: 2981
          targetPort: 8013
      selector:
        app: cloudstate-rest-connector
    ```
* Deploy service

    ```
        kubectl apply -n cloudstate -f src/main/resources/connector.yaml
    
    ```

### Consume connector service

* As a connector as service, you need to deploy in your **k8s** node the yaml file of the connector.

* Add in your **proto** folder the connector proto file, to create client code as **DSL** of the connector.
So then you can use in your service the client code to make the gRPC call.

```

    private RestConnectorGrpc.RestConnectorBlockingStub restConnector =
            RestConnectorGrpc.newBlockingStub(ManagedChannelBuilder
                    .forAddress("cloudstate-rest-connector-service.cloudstate", 2981)
                    .usePlaintext(true)
                    .build());
                    
    private String makeRestConnectorCall() {

        RestResponse response = restConnector.makeRequest(RestRequest.newBuilder()
                .setHost("www.mocky.io")
                .setPort(80)
                .setUri("/v2/5185415ba171ea3a00704eed")
                .setMethod(Method.GET)
                .build());

        return  response.getResponse();
    }

```


