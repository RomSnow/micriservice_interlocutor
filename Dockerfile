FROM hseeberger/scala-sbt:8u282_1.4.7_2.11.12 as build

ARG SONAR_HOST_URL
ARG SONAR_LOGIN
ARG CI_BUILD_REF
ARG CI_BUILD_REF_NAME
ARG CI_PIPELINE_URL
ARG VERSION
ARG PRENAME

ENV SONAR_HOST_URL ${SONAR_HOST_URL}
ENV SONAR_LOGIN ${SONAR_LOGIN}
ENV CI_BUILD_REF ${CI_BUILD_REF}
ENV CI_BUILD_REF_NAME ${CI_BUILD_REF_NAME}
ENV CI_PIPELINE_URL ${CI_PIPELINE_URL}
ENV VERSION ${VERSION}
ENV PRENAME ${PRENAME}

WORKDIR /app
COPY . .

RUN if [ -z ${SONAR_HOST_URL} ] ; then sbt -mem 8196 -J-Xms2G -J-Xss3M clean stage; else sbt -mem 8196 -J-Xms2G -J-Xss3M clean stage sonarScan; fi

FROM openjdk:8-jdk-alpine

WORKDIR /app
ENV APP_PATH="/app"
RUN mkdir $APP_PATH/gc_logs

ENV RESOURCE_FILE_PATH="$APP_PATH/resources/" LOGGER_CONF_PATH=$APP_PATH/resources/logback.xml CONFIG_FILE_PATH=$APP_PATH/resources/application.conf
ENV GC_OPTS="-Xloggc:$APP_PATH/gc_logs/gc.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintTenuringDistribution -XX:+PrintGCCause -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=5M"
ENV MEMORY_OPTS="-Xms1024m -Xmx4G -Xss1M"
ENV JAVA_OPTS="$MEMORY_OPTS $GC_OPTS"

ENV CLASS_PATH="$APP_PATH/lib/*"
ENV LANG="en_US.UTF-8" LC_COLLATE="en_US.UTF-8" LC_CTYPE="en_US.UTF-8" LC_MESSAGES="en_US.UTF-8" LC_MONETARY="en_US.UTF-8" LC_NUMERIC="en_US.UTF-8" LC_TIME="en_US.UTF-8" LC_ALL="en_US.UTF-8"

COPY --from=build /app/src/main/resources $APP_PATH/resources
COPY --from=build /app/target/universal/stage/lib $APP_PATH/lib

ENTRYPOINT /usr/bin/java $JAVA_OPTS \
        -Dconfig.file=$CONFIG_FILE_PATH \
        -Dlogback.configurationFile=$LOGGER_CONF_PATH \
        -Dfile.encoding=UTF8 $@ \
        -cp "$CLASS_PATH" Main $APP_PATH