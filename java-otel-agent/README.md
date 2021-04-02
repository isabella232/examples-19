# java-otel-agent

## RUN
```sh
mvn package
OTEL_RESOURCE_ATTRIBUTES=service.name=app1 OTEL_METRICS_EXPORTER=none OTEL_EXPORTER_OTLP_ENDPOINT="https://api-dogfood.honeycomb.io:443" OTEL_EXPORTER_OTLP_HEADERS="x-honeycomb-team=abc,x-honeycomb-dataset=shiny-java" java -javaagent:opentelemetry-javaagent-all.jar -jar target/java-example-webapp-1.0.0.jar
```

## API

This application exposes basic REST API for todos on port 8080 (see the
[application.properties](src/main/resources/application.properties) if you would like to override this):

```sh
$ curl \
    -H 'Content-Type: application/json' \
    -X POST -d '{"description": "Walk the dog", "due": 1518816723, "completed": false}' \
    localhost:8080/todos/
...

$ curl localhost:8080/todos/
[
  {
    "completed": false,
    "description": "Walk the dog",
    "due": "Fri, 16 Feb 2018 21:32:03 GMT",
    "id": 1
  }
]

$ curl -X PUT \
    -H 'Content-Type: application/json' \
    -d '{"description": "Walk the cat", "due": 1518816723, "completed": false}' \
    localhost:8080/todos/1/
{
  "completed": false,
  "description": "Walk the cat",
  "due": "Fri, 16 Feb 2018 21:32:03 GMT",
  "id": 1
}

$ curl -X DELETE localhost:8080/todos/1/
{
  "id": 1,
  "success": true
}

$ curl localhost:8080/todos/
[]
```
