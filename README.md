# x2ora

Clone this repository.

    git clone https://github.com/blitzboard/x2ora.git

Set database information.

    cd ~/x2ora/src/main/resources/
    cp common.sample.properties common.properties
    vi common.properties

Run Gradle.

    cd ~/x2ora/
    sh restart.sh

Update requests.

```
curl -XPOST -H 'Content-Type: application/json' -d @sample/pg_named.json 'http://localhost:7000/create/'
curl -XPOST -H 'Content-Type: application/json' -d @sample/pg_named.json 'http://localhost:7000/update/'
curl -XPOST -d 'graph=e730fec3-680b-444a-8fec-0b801f5991db' -d 'name=test2' http://localhost:7000/rename
curl -XPOST -d 'graph=e730fec3-680b-444a-8fec-0b801f5991db' http://localhost:7000/drop
```

Retrieval requests.

```
curl -G 'http://localhost:7000/list/'
curl -G 'http://localhost:7000/get/' -d 'graph=e730fec3-680b-444a-8fec-0b801f5991db'
curl -G 'http://localhost:7000/query/' -d 'graph=e730fec3-680b-444a-8fec-0b801f5991db' --data-urlencode 'match=(n1)-[e]->(n2)' --data-urlencode "where=1=1"
curl -G 'http://localhost:7000/query_path/' --data-urlencode 'match=ANY (n1)->+(n2)' --data-urlencode "where=n1.id='台風'"
curl -G 'http://localhost:7000/query_table/' --data-urlencode 'query=SELECT v.id, COUNT(*) AS cnt FROM MATCH (v) GROUP BY v.id ORDER BY cnt DESC'
```
