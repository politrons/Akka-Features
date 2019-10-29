# CloudState

### Service

You can implement your service using the current DSL.

Here I implement an Event sourcing service using as Data Store the **InMemory** option

[Event sourcing](CloudState/src/main/java/io/cloudstate/shopping/ShoppingCartEntity.java)
    
### Docker

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
                            <name>politrons/shopping-cart:%l</name>
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
                                    <arg>io.cloudstate.shopping.ShoppingCartMain</arg>
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

    Using in the plugin ``<name>politrons/shopping-cart:%l</name>``` it means it will be publish in my personal docker hub **politrons**

* Create image ``Docker build``
* Push image ``Docker push``


### K8s

* Create namespace

    ```
    kubectl create namespace cloudstate
    ```
    
* Download cloudstate.yaml yaml file that you can find [here](https://github.com/cloudstateio/cloudstate/tags)
    

* Add in the yaml file your service to be deployed

    ```
    ---
    apiVersion: cloudstate.io/v1alpha1
    kind: StatefulService
    metadata:
      name: shopping-cart
    spec:
      datastore:
        name: inmemory
      containers:
        - image: politrons/shopping-cart:latest
    ```

* Create Operator in new namespace

```
    kubectl apply -n cloudstate -f src/main/resource/cloudstate.yaml

```



