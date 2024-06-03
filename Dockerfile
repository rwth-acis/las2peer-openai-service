FROM openjdk:17-jdk-alpine

ENV HTTP_PORT=8080
ENV HTTPS_PORT=8443

RUN apk add --update bash tzdata curl && rm -f /var/cache/apk/*

COPY --chown=las2peer:las2peer . /src
WORKDIR /src

# Include this in case you build on a windows machine
RUN dos2unix gradlew
RUN dos2unix gradle.properties
RUN dos2unix /src/docker-entrypoint.sh

RUN chmod -R a+rwx /src
RUN chmod +x /src/docker-entrypoint.sh
RUN chmod +x gradlew && ./gradlew build 

EXPOSE $HTTP_PORT
EXPOSE $HTTPS_PORT

RUN chmod +x /src/docker-entrypoint.sh
ENTRYPOINT ["/src/docker-entrypoint.sh"]