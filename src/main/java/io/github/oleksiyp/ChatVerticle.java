package io.github.oleksiyp;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

import static java.util.Collections.synchronizedSet;

public class ChatVerticle extends AbstractVerticle {
    public static final String KAFKA = "localhost:32775";
    public static final String CHAT_TOPIC = "chat";
    public static final Logger LOGGER = LoggerFactory.getLogger(ChatVerticle.class);

    Consumer<String> kafkaChatTopic;
    Set<Consumer<String>> webSockets = synchronizedSet(new HashSet<>());

    @Override
    public void start() throws Exception {
        startKafkaConsumer(KAFKA, CHAT_TOPIC, this::sendWebsockets);

        kafkaChatTopic = startKafkaProducer(KAFKA, CHAT_TOPIC);

        startWeb();
    }

    private void sendWebsockets(String message) {
        webSockets.forEach((webSocket) ->
                webSocket.accept(message));
    }

    public void startWeb() {
        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);
        router.route("/").handler(StaticHandler.create());

        startSocksJS(router);

        server.requestHandler(router::accept)
                .listen(8080);
    }

    public void startSocksJS(Router router) {
        router.route("/chat/*")
                .handler(SockJSHandler.create(vertx)
                        .socketHandler(socket -> {
                            Consumer<String> consumer = socket::write;
                            webSockets.add(consumer);
                            socket.endHandler((s) -> webSockets.remove(consumer));
                            socket.handler((buf) ->
                                    kafkaChatTopic.accept(buf.toString()));
                        }));
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
                    sendWebsockets("Failed to send: " + message);
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
        config.put("auto.offset.reset", "earliest");
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
