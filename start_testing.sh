#!/bin/bash

containerName='microservice-model';

while getopts n:t: flag
do
    case "${flag}" in
        n) count=${OPTARG};;
        t) test_type=${OPTARG};;
    esac
done

# clear environment
if [[ $(docker ps -aq) ]]; then
  docker stop $(docker ps -aq);
  docker rm $(docker ps -aq);
fi
sudo rm -rf ~/containers_data/;
mkdir ~/containers_data;
#docker rmi microservice_interlocutor;
docker network rm sandbox;

# build image
docker build -t microservice_interlocutor . ;

# create network
docker network create --driver bridge \
  --subnet 192.168.0.0/24 sandbox;

# start kafka
if [[ $test_type == 'kafka' ]]; then
  echo "start kafka";
  topics="broadcast-topic:1:1"
  for i in $(seq 1 $count); do
    topics+=", ${containerName}-${i}-topic:1:1"
  done;

  docker run -d --net sandbox -p "2181:2181" --name "zookeeper" wurstmeister/zookeeper;
  docker run -d --net sandbox -p "9092:9092" \
    -e KAFKA_ADVERTISED_HOST_NAME=kafka \
    -e KAFKA_CREATE_TOPICS="$topics" \
    -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
    -v ./:/etc/kafka \
    --name "kafka" wurstmeister/kafka;
fi

#start containers

for i in $(seq 1 $count); do
  echo "${containerName}-${i}";
  docker run -d --net sandbox --expose=80 --env-file .env \
   -e INTERLOCUTOR_SELF_NUMBER=$i \
   -e INTERLOCUTORS_COUNT=$count \
   -e INTERLOCUTORS_NAME=$containerName \
   -e STAT_FILE_FULL_NAME=/data/result.txt \
   -e SERVICE_TYPE=$test_type \
   -v ~/containers_data/$i:/data \
   --name "${containerName}-${i}" microservice_interlocutor;
done


