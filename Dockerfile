FROM openjdk:11-jre-slim as BUILD_IMAGE
WORKDIR /extractor/

COPY gradle gradle
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY src src

RUN ./gradlew build -x test

FROM BUILD_IMAGE
WORKDIR /extractor/
COPY --from=BUILD_IMAGE /extractor/build/libs/extractor-1.0.jar .
EXPOSE 8080 8081
ENTRYPOINT java $JAVA_OPTS -jar extractor-1.0.jar
