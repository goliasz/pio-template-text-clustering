# pio-template-text-clustering

Text clustering service based on Word2Vec and KMeans algorithms. Builds vectors of full documents in training phase. Finds similar documents in query phase. Finds cluster number of queried doc.

## Docker Part

docker pull goliasz/docker-predictionio<br>
cd $HOME<br>
mkdir MyEngine<br>
docker run --hostname pio1 --privileged=true --name pio1 -it -p 8000:8000 -p 7070:7070 -p 7071:7071 -p 7072:7072 -v $HOME/MyEngine:/MyEngine goliasz/docker-predictionio /bin/bash<br>

## PIO Part

root@pio1:/# pio-start-all<br>
root@pio1:/# cd MyEngine<br>
root@pio1:/MyEngine# pio template get goliasz/pio-template-text-clustering --version "0.2" textclus<br>
root@pio1:/MyEngine/textclus# vi engine.json<br>
Set application name to “textclus”<br>
<br>
root@pio1:/MyEngine/textclus# pio build --verbose<br>
root@pio1:/MyEngine/textclus# pio app new textclus<br>
root@pio1:/MyEngine/textclus# sh ./data/import_test.sh [YOUR APP ID from "pio app new textclus" output]<br>
root@pio1:/MyEngine/textclus# pio train<br>
root@pio1:/MyEngine/textclus# pio deploy --port 8000 &<br>

### Test Event Server Status

curl -i -X GET http://localhost:7070<br>

### Event Server: get all events

curl -i -X GET http://localhost:7070/events.json?accessKey=[YOUR ACCESS KEY FROM "pio app new textclus" output]<br>
<br>
### Query cluster and similarity scores

curl -X POST -H "Content-Type: application/json" -d '{"doc": "Everyone realizes why a new common language would be desirable", "limit", 3}' http://localhost:8000/queries.json<br>
<br>
Result:<br>
<br>
{<br>
"cluster":2.0,<br>
"docScores":<br>
   [<br>
{"cluster":2.0,"score":0.40422985696997243,"id":"2"},<br>
{"cluster":3.0,"score":0.19822372027235752,"id":"5"},<br>
{"cluster":1.0,"score":0.08433911378575301,"id":"4"}<br>
   ]<br>
}<br>



