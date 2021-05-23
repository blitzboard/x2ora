# x2oracle

Clone this repository.

    $ git clone https://github.com/ryotayamanaka/x2oracle.git

Get Oracle Graph Client library.

* [Oracle Graph Client](https://www.oracle.com/database/technologies/spatialandgraph/property-graph-features/graph-server-and-client/graph-server-and-client-downloads.html)

Locate and unzip in `x2oracle` directory.

    $ cd x2oracle
    $ unzip oracle-graph-client-21.2.0.zip

Set database login information.

    $ cd src/main/resource
    $ mv common.sample.properties common.properties
    $ vi common.properties

Run Gradle.

    $ ./gradlew run

Update requests.

http://localhost:7000/merge_node/?label=person&id=Taro&props={"age":[37]}
http://localhost:7000/merge_node/?label=person&id=Jiro&props={"age":[36]}
http://localhost:7000/merge_edge/?label=knows&src_id=Taro&dst_id=Jiro&props={"since":[2017]}

Retrieval requests.

curl http://localhost:7000/node_match/?id=Taro
curl http://localhost:7000/edge_match/?labels=knows
