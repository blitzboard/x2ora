# x2ora

Clone this repository.

    $ git clone https://github.com/g2glab/x2ora.git

Download Oracle Graph Client library.

* [Oracle Graph Client](https://www.oracle.com/database/technologies/spatialandgraph/property-graph-features/graph-server-and-client/graph-server-and-client-downloads.html)

Locate and unzip the library under `x2ora` directory.

    $ cd ~/x2ora/
    $ unzip oracle-graph-client-21.2.0.zip

Set database login information.

    $ cd ~/x2orasrc/main/resources/
    $ cp common.sample.properties common.properties
    $ vi common.properties

Run Gradle.

    $ cd ~/x2ora/
    $ ./gradlew run

FYI, how to modify the firewall setting to open the port 7000.

    $ sudo firewall-cmd --list-all
    $ sudo firewall-cmd --zone=public --add-port=7000/tcp --permanent
    $ sudo firewall-cmd --reload

Update requests.

    curl http://localhost:7000/merge_node/?label=person&id=Taro&props={"age":[37]}
    curl http://localhost:7000/merge_node/?label=person&id=Jiro&props={"age":[36]}
    curl http://localhost:7000/merge_edge/?label=knows&src_id=Taro&dst_id=Jiro&props={"since":[2017]}

Retrieval requests.

    curl http://localhost:7000/node_match/?id=Taro
    curl http://localhost:7000/edge_match/?labels=knows

