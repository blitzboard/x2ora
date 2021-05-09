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

Try requests.

* http://localhost:7000/merge_node/?label=PERSON&id=Taro
* http://localhost:7000/merge_edge/?label=KNOWS&src_id=Taro&dst_id=Jiro