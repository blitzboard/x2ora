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

FYI, how to modify the firewall setting to open the port 7000 and 7001.

    $ sudo firewall-cmd --list-all
    $ sudo firewall-cmd --zone=public --add-port=7000/tcp --permanent
    $ sudo firewall-cmd --zone=public --add-port=7001/tcp --permanent
    $ sudo firewall-cmd --reload

Update requests.

    curl "http://localhost:7000/merge_node/?label=person&id=Taro&props={"age":[37]}"
    curl "http://localhost:7000/merge_node/?label=person&id=Jiro&props={"age":[36]}"
    curl "http://localhost:7000/merge_edge/?label=knows&src_id=Taro&dst_id=Jiro&props={"since":[2017]}"

Retrieval requests.

    curl "http://localhost:7000/node_match/?node_ids[]=Taro&limit=100"
    curl "http://localhost:7000/edge_match/?edge_labels[]=knows&limit=100"

## Supported API

The full X2 API is described at https://g2glab.github.io/x2/. However, x2ora supports the subset of the API below.

- /node_match
  - node_ids
  - limit
- /edge_match
  - edge_labels
  - limit

## Configure SSL

Recreate the keystore setting the IP address `<ip_address>`.

    rm ./src/main/resources/keystore.jks
    keytool -genkey \
    -dname "cn=<ip_address>, ou=MyTeam, o=MyOrg, l=MyCity, st=MyState, c=TH" \
    -alias jetty -keystore ./src/main/resources/keystore.jks -storepass welcome1 -keypass welcome1 \
    -keyalg RSA -keysize 2048 -sigalg SHA256withRSA -validity 3650 -ext SAN=dns:<ip_address> -storetype pkcs12

Export the certificate.

    keytool -export \
    -alias jetty -storepass welcome1 -file x2ora.crt -keystore ./src/main/resources/keystore.jks

Enable the certificate in the client computer.

- Open the certificate file `x2ora.crt`
- Import into the Keychain (Mac OS)
- Open the entry and set "Always Trust" at Trust tab 

