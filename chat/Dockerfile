# Extend vert.x image
FROM openjdk:8-jre-alpine

ENV VERTICLE_NAME io.github.oleksiyp.ChatVerticle
ENV VERTICLE_FILE vertx-kafka-chat-1.0-SNAPSHOT-jar-with-dependencies.jar
ENV VERTICLE_HOME /usr/verticles

EXPOSE 8080

# Copy your fat jar to the container
COPY target/$VERTICLE_FILE $VERTICLE_HOME/

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java -jar $VERTICLE_FILE"]