package io.github.oleksiyp;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSSocket;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

public class ChatVerticle extends AbstractVerticle {
    public static final Logger LOGGER = LoggerFactory.getLogger(ChatVerticle.class);
    public static final String KAFKA_HOST_PORT = "localhost:9092";
    public static final String CHAT_TOPIC = "chat";

    private Consumer<String> broadcast;
    private Set<Consumer<String>> webClients;

    @Override
    public void start() throws Exception {
        String kafkaServer = System.getenv().getOrDefault("KAFKA", KAFKA_HOST_PORT);

        webClients = new ConcurrentHashSet<>();

        startKafkaConsumer(kafkaServer, CHAT_TOPIC, this::sendWebClients);

        broadcast = startKafkaProducer(kafkaServer, CHAT_TOPIC);

        startWeb();
    }

    public void startWeb() {
        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);
        router.route("/chat/*").handler(creatChatWebSocketHandler());
        router.route("/*").handler(StaticHandler.create());

        server.requestHandler(router::accept)
                .listen(8080);
    }

    public Handler<RoutingContext> creatChatWebSocketHandler() {
        return SockJSHandler.create(vertx)
                .socketHandler(this::onNewSocket);
    }

    private void onNewSocket(SockJSSocket socket) {
        Consumer<String> consumer = socket::write;
        webClients.add(consumer);
        socket.endHandler((s) -> webClients.remove(consumer));
        socket.handler((buf) ->
                broadcast.accept(buf.toString()));
    }

    private void sendWebClients(String message) {
        webClients.forEach((client) ->
                client.accept(message));
    }

    public Consumer<String> startKafkaProducer(String server, String topic) {
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", server);
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("linger.ms", "0");
        config.put("request.timeout.ms", "500");
        config.put("acks", "1");

        // use producer for interacting with Apache Kafka
        KafkaProducer<String, String> producer = KafkaProducer.create(vertx, config);
        return message -> {
            KafkaProducerRecord<String, String> record;
            record = KafkaProducerRecord.create(topic, message);
            producer.write(record, (r) -> {
                if (!r.succeeded()) {
                    sendWebClients("Failed to send: " + message);
                    LOGGER.warn("Failed to send", r.cause());
                }
            });
        };
    }

    public void startKafkaConsumer(String server, String topic, Consumer<String> messageConsumer) {
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", server);
        config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("group.id", "chat");
        config.put("auto.offset.reset", "latest");
        config.put("enable.auto.commit", "false");

        // use consumer for interacting with Apache Kafka
        KafkaConsumer<String, String> consumer = KafkaConsumer.create(vertx, config);
        consumer.handler((record) -> messageConsumer.accept(record.value()));
        consumer.subscribe(topic);
    }

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new ChatVerticle());
    }
}
