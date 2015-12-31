# pio-template-text-clustering

Text clustering service based on Word2Vec and KMeans algorithms. Builds vectors of full documents in training phase. Finds similar documents in query phase. Finds cluster number of queried doc.

## Docker Part

docker pull goliasz/docker-predictionio
cd $HOME
mkdir MyEngine
docker run --hostname pio1 --privileged=true --name pio1 -it -p 8000:8000 -p 7070:7070 -p 7071:7071 -p 7072:7072 -v $HOME/MyEngine:/MyEngine goliasz/docker-predictionio /bin/bash

## PIO Part

root@pio1:/# pio-start-all
root@pio1:/# cd MyEngine
root@pio1:/MyEngine# pio template get goliasz/pio-template-text-clustering --version "0.1" textclus
root@pio1:/MyEngine/textclus# vi engine.json
Set application name to “textclus”

root@pio1:/MyEngine/textclus# pio build --verbose
root@pio1:/MyEngine/textclus# pio app new textclus
root@pio1:/MyEngine/textclus# sh ./data/import_test.sh [YOUR APP ID from "pio app new textclus" output]
root@pio1:/MyEngine/textclus# pio train
root@pio1:/MyEngine/textclus# pio deploy --port 8000

### Test Event Server Status

curl -i -X GET http://localhost:7070

### Event Server: get all events

curl -i -X GET http://localhost:7070/events.json?accessKey=[YOUR ACCESS KEY FROM "pio app new textclus" output]


