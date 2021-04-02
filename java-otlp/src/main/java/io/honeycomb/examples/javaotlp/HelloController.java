package io.honeycomb.examples.javaotlp;

import io.opentelemetry.api.*;
import io.opentelemetry.api.baggage.*;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.*;
import io.opentelemetry.context.propagation.*;
import org.springframework.web.bind.annotation.*;

import javax.annotation.*;
import java.io.*;
import java.net.*;
import java.util.*;

import static io.opentelemetry.api.GlobalOpenTelemetry.*;

@RestController
public class HelloController {

    @RequestMapping("/")
    public String index(@RequestHeader Map<String, String> headers) {
        final TextMapPropagator textMapPropagator = getPropagators().getTextMapPropagator();
        Context ctx = textMapPropagator.extract(Context.current(), headers, new TextMapGetter<>() {
            @Override
            public Iterable<String> keys(Map<String, String> carrier) {
                return carrier.keySet();
            }

            @Nullable
            @Override
            public String get(@Nullable Map<String, String> carrier, String key) {
                if (carrier != null) {
                    return carrier.get(key);
                } else {
                    return null;
                }
            }
        });

        try (Scope scope = ctx.with(Baggage.fromContext(ctx).toBuilder().put("foof", "20s").build()).makeCurrent()) {
            Span span = GlobalOpenTelemetry.getTracer("java-otlp")
                .spanBuilder("hello")
                .startSpan();
            try {
                final URL url = new URL("http://localhost:8080/todos");
                final HttpURLConnection transportLayer = (HttpURLConnection) url.openConnection();
                getPropagators().getTextMapPropagator().inject(Context.current(), transportLayer, URLConnection::setRequestProperty);
                try {
                    transportLayer.connect();
                    final int responseCode = transportLayer.getResponseCode();
                    System.out.println("got response: " + responseCode);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                span.end();
            }
        }
        return "Hello world!";
    }

}
