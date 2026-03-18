FROM eclipse-temurin:21

RUN mkdir /opt/app

COPY /build/libs/epub-library-api-1.0.0.jar /opt/app/app.jar

CMD [ "java", "-jar", "/opt/app/app.jar" ]