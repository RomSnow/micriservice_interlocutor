#!/bin/bash

containerName='microservice_model';

while getopts n: flag
do
    case "${flag}" in
        n) count=${OPTARG};;
    esac
done

# clear environment
if [[ $(docker ps -aq) ]]; then
  docker stop $(docker ps -aq);
  docker rm $(docker ps -aq);
fi
#docker rmi microservice_interlocutor;
docker network rm sendbox;

# build image
#docker build -t microservice_interlocutor .;

# create network

docker network create --driver bridge \
  --subnet 192.168.0.0/16 sandbox;

#start containers

for i in $(seq 1 $count); do
  echo "${containerName}_${i}";
  docker run -d --net sandbox --expose=80 --env-file .env \
   -e INTERLOCUTORS_COUNT=$count \
   -e INTERLOCUTORS_NAME=$containerName --name "${containerName}_${i}" microservice_interlocutor;
done


