FROM amazoncorretto:21-alpine
COPY ./build/libs/schedule-1.0.jar /tmp/schedule.jar
WORKDIR /tmp
ENTRYPOINT ["java","-jar","schedule.jar"]