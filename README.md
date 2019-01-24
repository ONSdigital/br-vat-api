# Business Register VAT API
[![license](https://img.shields.io/github/license/mashape/apistatus.svg)](./LICENSE)

Supports retrieval of Value Added Tax (VAT) admin data.

See the [Open API Specification](./api.yaml) for details of the API.


### Development Tasks

Run the service in development mode (default Play port of 9000):

    sbt run

Run the service in development mode (custom port of 9123):

    sbt "run 9123"

Run unit tests with coverage:

    sbt clean coverage test coverageReport

Run acceptance tests:

    sbt it:test

Run all tests:

    sbt clean test it:test

Generate static analysis report:

    sbt scapegoat


#### Testing Against a Local HBase Database

1.  Start HBase

        bin/start-hbase.sh

2.  Create Database Schema

        bin/hbase shell

    In the resulting HBase Shell, create the namespace:

        create_namespace 'br_vat_db'

    followed by the table:

        create 'br_vat_db:vat', 'd', 'h'

3.  Create Sample Data

    Create one record with only mandatory fields:

        put 'br_vat_db:vat', '123456789012', 'd:vatref', '123456789012-vatref'
        put 'br_vat_db:vat', '123456789012', 'd:nameline1', '123456789012-nameline1'
        put 'br_vat_db:vat', '123456789012', 'd:legalstatus', '123456789012-legalstatus'
        put 'br_vat_db:vat', '123456789012', 'd:address1', '123456789012-address1'
        put 'br_vat_db:vat', '123456789012', 'd:postcode', '123456789012-postcode'
        put 'br_vat_db:vat', '123456789012', 'd:sic92', '123456789012-sic'
        put 'br_vat_db:vat', '123456789012', 'd:recordtype', '123456789012-recordtype'

    And another with all possible fields:

        put 'br_vat_db:vat', '987654321012', 'd:vatref', '987654321012-vatref'
        put 'br_vat_db:vat', '987654321012', 'd:nameline1', '987654321012-nameline1'
        put 'br_vat_db:vat', '987654321012', 'd:nameline2', '987654321012-nameline2'
        put 'br_vat_db:vat', '987654321012', 'd:nameline3', '987654321012-nameline3'
        put 'br_vat_db:vat', '987654321012', 'd:legalstatus', '987654321012-legalstatus'
        put 'br_vat_db:vat', '987654321012', 'd:address1', '987654321012-address1'
        put 'br_vat_db:vat', '987654321012', 'd:address2', '987654321012-address2'
        put 'br_vat_db:vat', '987654321012', 'd:address3', '987654321012-address3'
        put 'br_vat_db:vat', '987654321012', 'd:address4', '987654321012-address4'
        put 'br_vat_db:vat', '987654321012', 'd:address5', '987654321012-address5'
        put 'br_vat_db:vat', '987654321012', 'd:postcode', '987654321012-postcode'
        put 'br_vat_db:vat', '987654321012', 'd:sic92', '987654321012-sic'
        put 'br_vat_db:vat', '987654321012', 'd:recordtype', '987654321012-recordtype'
        put 'br_vat_db:vat', '987654321012', 'd:tradstyle1', '987654321012-tradstyle1'
        put 'br_vat_db:vat', '987654321012', 'd:tradstyle2', '987654321012-tradstyle2'
        put 'br_vat_db:vat', '987654321012', 'd:tradstyle3', '987654321012-tradstyle3'
        put 'br_vat_db:vat', '987654321012', 'd:birthdate', '987654321012-birthdate'
        put 'br_vat_db:vat', '987654321012', 'd:deathdate', '987654321012-deathdate'
        put 'br_vat_db:vat', '987654321012', 'd:deathcode', '987654321012-deathcode'
        put 'br_vat_db:vat', '987654321012', 'd:turnover', '1000'
        put 'br_vat_db:vat', '987654321012', 'd:turnoverdate', '987654321012-turnoverdate'
        put 'br_vat_db:vat', '987654321012', 'd:ubrn', '987654321012-ubrn'

4.  Start HBase Rest Service

        bin/hbase rest start

5.  Start This Service

        export BR_VAT_QUERY_DB_HBASE_NAMESPACE=br_vat_db
        export BR_VAT_QUERY_DB_HBASE_TABLE_NAME=vat

        sbt clean run

    This runs the service on the default Play port (9000).

6.  Query This API

        curl -i http://localhost:9000/v1/vat/123456789012
        curl -i http://localhost:9000/v1/vat/987654321012

7.  Clerically edit a unit (modifying the existing UBRN)

        curl -i \
        -H "Content-Type: application/json-patch+json" \
        -H "X-User-Id: bloggsj" \
        -d '[{"op": "test", "path": "/links/ubrn", "value": "987654321012-ubrn"},
             {"op": "replace", "path": "/links/ubrn", "value": "1234567890123456"}]' \
        -X PATCH \
        "http://localhost:9000/v1/vat/987654321012"

8.  Shutdown This Service

    Terminate the running command (typically Ctrl-C).

9.  TearDown Database

    Delete the table:

        disable 'br_vat_db:vat'
        drop 'br_vat_db:vat'

    followed by the namespace:

        drop_namespace 'br_vat_db'

10.  Shutdown HBase REST Service

        bin/hbase rest stop

11.  Shutdown HBase

         bin/hbase stop-hbase.sh


### API Specification
The `api.yaml` file in the root project directory documents the API using the [Open API Specification](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.2.md).
This file is best edited using [Swagger Editor](https://github.com/swagger-api/swagger-editor) and best viewed using [Swagger UI](https://github.com/swagger-api/swagger-ui/).
The Docker approach outlined [here](https://github.com/swagger-api/swagger-editor#docker) seems to work well.


### Tracing
[kamon](http://kamon.io) is used to automatically instrument the application and report trace spans to
[zipkin](https://zipkin.io/).  The AspectJ Weaver is required to make this happen, see [adding-the-aspectj-weaver](http://kamon.io/documentation/1.x/recipes/adding-the-aspectj-weaver/)
for further details.

Kamon takes care of propagating the traceId across threads, and making the relevant traceId available to
logback's Mapped Diagnostic Context (MDC).

Tracing is not enabled during the execution of tests, resulting in log statements that contain a traceId
with the value "undefined".

To undertake manual trace testing, run a local Zipkin 2 server.  One simple way to do this is via Docker:

    docker run --rm -d -p 9411:9411 openzipkin/zipkin:2.11

Then run this service via `sbt run`, and exercise an endpoint.

The resulting trace information should be available in the Zipkin UI at
[http://localhost:9411/zipkin/](http://localhost:9411/zipkin/).


### Service Configuration
As is standard for Play, the runtime configuration file can be found at `src/main/resources/application.conf`.

This file adopts a pattern where each variable has a sensible default for running the application locally,
which may then be overridden by an environment variable (if defined).  For example:

    host = "localhost"
    host = ${?BR_VAT_QUERY_DB_HBASE_HOST}

The actual settings used for our formal deployment environments are held outside of Github, and rely on the
the ability to override settings via environment variables in accordance with the '12-factor app' approach.

Note that acceptance tests (and the entire IntegrationTest phase generally) use a dedicated configuration
that is defined at `src/it/resources/it_application.conf`.  This imports the standard configuration, and then
overrides the environment to that expected by locally executing acceptance tests.  This allows us to specify
non-standard ports for example, to avoid conflicts with locally running services.  For this to work, the
build file overrides the `-Dconfig.resource` setting when executing the IntegrationTest phase.


### Edit History
A history of edits is maintained in a separate 'h' column family.

For each edit a new history cell is created.  The column name of this cell is a UUID, and the cell value
has the format:

    {userId}~{iso-local-date-time}~{old-ubrn}~{new-ubrn}

For example:

    column=h:90731d6b-d98f-450b-871d-806d13901953,
    value=bloggsj~2019-01-23T09:22:39.264~1234567890123456~9999999999999999

Metadata for the edit (currently the editedBy userId) must be supplied by HTTP Headers (currently `X-User-Id`).
Any `PATCH` request without the required metadata headers will receive a `Bad Request (400)` response.
