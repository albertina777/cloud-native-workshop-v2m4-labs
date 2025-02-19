package com.redhat.cloudnative;

import com.redhat.cloudnative.model.order.Order;
import com.redhat.cloudnative.model.ShoppingCart;
import com.redhat.cloudnative.service.ShoppingCartService;

import io.vertx.core.json.Json;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.*;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.StartupEvent;

import java.util.Properties;


@Path("/api/cart")
public class CartResource {

    private static final Logger log = LoggerFactory.getLogger(CartResource.class);

    // TODO: Add annotation of orders messaging configuration here
   @ConfigProperty(name = "mp.messaging.outgoing.orders.bootstrap.servers") public String bootstrapServers;
   @ConfigProperty(name = "mp.messaging.outgoing.orders.topic") public String ordersTopic;
   @ConfigProperty(name = "mp.messaging.outgoing.orders.value.serializer") public String ordersTopicValueSerializer;
   @ConfigProperty(name = "mp.messaging.outgoing.orders.key.serializer") public String ordersTopicKeySerializer;
    private Producer<String, String> producer;

    @Inject
    ShoppingCartService shoppingCartService;

    // TODO ADD getCart method
    @GET
    @Path("{cartId}")
    public ShoppingCart getCart(String cartId) {
       return shoppingCartService.getShoppingCart(cartId);
    }

    @POST
    @Path("{cartId}/{itemId}/{quantity}")
    public ShoppingCart add(String cartId, String itemId, int quantity) throws Exception {
        return shoppingCartService.addItem(cartId, itemId, quantity);
    }

    @POST
    @Path("{cartId}/{tmpId}")
    public ShoppingCart set(String cartId, String tmpId) throws Exception {
        return shoppingCartService.set(cartId, tmpId);
    }

    @DELETE
    @Path("{cartId}/{itemId}/{quantity}")
    public ShoppingCart delete(String cartId, String itemId, int quantity) throws Exception {
        return shoppingCartService.deleteItem(cartId, itemId, quantity);
    }

   @POST
   @Path("/checkout/{cartId}")
   public ShoppingCart checkout(@PathParam("cartId") String cartId, Order order) {
      sendOrder(order, cartId);
     return shoppingCartService.checkout(cartId); 
    }

    // TODO ADD for KAFKA
    private void sendOrder(Order order, String cartId) {
order.setTotal(shoppingCartService.getShoppingCart(cartId).getCartTotal() + "");
ProducerRecord<String, String> producerRecord = new ProducerRecord<>(ordersTopic, null, null, null, Json.encode(order), new
RecordHeaders().add("content-type", "application/json".getBytes()));
        producer.send(producerRecord);
        log.info("Sent message: " + Json.encode(order));
    }

    // TODO ADD for KAFKA
   public void init(@Observes StartupEvent ev) { 
       Properties props = new Properties();
props.put("bootstrap.servers", bootstrapServers); props.put("value.serializer", ordersTopicValueSerializer); props.put("key.serializer", ordersTopicKeySerializer); producer = new KafkaProducer<String, String>(props);
}

}
