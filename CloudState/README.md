# CloudState

    
### Docker

* Plugin to Create and push image into docker hub

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

* Create image ``Docker build``
* Push image ``Docker push``



### K8s

* Create namespace

    ```
    kubectl create namespace cloudstate
    ```
    
* Add service description in yaml file
    

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



