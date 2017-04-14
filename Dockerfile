# Extend vert.x image
FROM vertx/vertx3

ENV VERTICLE_NAME io.github.oleksiyp.ChatVerticle
ENV VERTICLE_FILE target/vertx-kafka-chat-1.0-SNAPSHOT.jar
ENV VERTICLE_HOME /usr/verticles

EXPOSE 8080

COPY $VERTICLE_FILE $VERTICLE_HOME/

WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec vertx run $VERTICLE_NAME -cp $VERTICLE_HOME/*"]
