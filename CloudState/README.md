# CloudState

### Service

You can implement your service using the current [Cloudstate DSL](https://cloudstate.io/docs/user/features/index.html).

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


* Delete namespace

    ```
    kubectl delete namespace cloudstate
    ```
    
* Create namespace

    ```
    kubectl create namespace cloudstate
    ```
    
* Download **cloudstate** yaml file that you can find [here](https://github.com/cloudstateio/cloudstate/tags) and deploy it.

    ```
        kubectl apply -n cloudstate -f src/main/resources/cloudstate.yaml
    
    ```
    

* Add in the **service** yaml file the ``StatefulStore``, ``StatefulService`` and the ``Service`` to route connections from outside the cluster

    ```
    apiVersion: cloudstate.io/v1alpha1
    kind: StatefulStore
    metadata:
      name: inmemory
    spec:
      type: InMemory
    ---
    
    apiVersion: cloudstate.io/v1alpha1
    kind: StatefulService
    metadata:
      name: cloudstate-shopping-cart
      labels:
        app: cloudstate-shopping-cart
    spec:
      datastore:
        name: inmemory
      containers:
        - image: politrons/shopping-cart:latest
    ---
    
    apiVersion: v1
    kind: Service
    metadata:
      name: cloudstate-shopping-cart-service
    spec:
      type: LoadBalancer
      ports:
        - port: 1981
          targetPort: 8013
      selector:
        app: cloudstate-shopping-cart
    ```
* Deploy service

    ```
        kubectl apply -n cloudstate -f src/main/resources/service.yaml
    
    ```



### Consume service

* Get cart
    ```
    curl http://localhost:1981/carts/politrons
    
    ```
* Add products
    ```
    curl -X POST -H 'Content-Type: application/json' http://localhost:1981/cart/politrons/items/add -d '{"product_id":"uuidCode","name":"coca-cola","quantity":1}'
    
    ```
* Get item
    ```
    curl http://localhost:1981/cart/uuidCode
    
    ```    
* Delete product
    ```
    curl http://localhost:1981/cart/politrons/items/uuidCode/remove
    
    ```
