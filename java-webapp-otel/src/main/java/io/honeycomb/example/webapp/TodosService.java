package io.honeycomb.example.webapp;

import io.honeycomb.example.webapp.persistence.*;
import io.opentelemetry.api.baggage.*;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.*;
import io.opentelemetry.extension.annotations.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.function.*;

import static io.opentelemetry.api.GlobalOpenTelemetry.*;

/**
 * Wraps the Todos repository and times calls the database calls. Metrics on the call time are submitted to
 * Honeycomb.
 * <p>
 * This is a simple way of producing the metrics; in a more complex application, it would be preferable to use
 * AOP. See https://github.com/spring-projects/spring-data-examples/tree/master/jpa/interceptors/src/main/java/example/springdata/jpa/interceptors
 * for example.
 */
@Component
public class TodosService {
    @Autowired
    private TodoRepository todoRepository;

    public List<Todo> readTodos() {
        System.out.println("reading todos");
        Span span = Span.current();
        span.setAttribute("isRead", true);
        return timeCall(() -> todoRepository.findAll(), "timers.db.select_all_todos");
    }

    public void deleteTodo(final Long id) {
        System.out.println("deleting todos");
        foobar();
        timeCall(() -> todoRepository.delete(id), "timers.db.delete_todo");
    }

    public void updateTodo(final Long id, final Todo update) {
        System.out.println("updating todos");
        timeCall(() -> {
            final Todo todo = todoRepository.getOne(id);
            todo.setCompleted(update.getCompleted());
            todo.setDescription(update.getDescription());
            todo.setDue(update.getDue());
            todoRepository.save(todo);
        }, "timers.db.update_todo");
    }

    public void createTodo(final Todo todo) {
        System.out.println("creating todos");
        try (Scope scope = Context.current().with(Baggage.current().toBuilder().put("decade", "90s").build()).makeCurrent()) {
            assert Context.current() == Context.current().with(Baggage.current().toBuilder().put("decade", "90s").build());
            Span.current().setAttribute("decade", "90s");

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://localhost:7070"))
                .header("accept", "application/json")
                .build();
            try {
                client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

            timeCall(() -> todoRepository.save(todo), "timers.db.insert_todo");
        }
    }

    @WithSpan(value = "holler")
    private void holler() {
        Baggage.current().forEach((s, entry) -> Span.current().setAttribute(s, entry.getValue()));
    }

    private void foobar() {
        Tracer tracer = getTracer("foobar-lib");
        Span span = tracer.spanBuilder("foobar").startSpan();
        System.out.println("POPOPOP");
        span.setAttribute("isRead", false);
        span.end();
    }

    private <T> T timeCall(final Supplier<T> call, final String callName) {
        final long startTime = System.currentTimeMillis();
        try {
            return call.get();
        } finally {
            final long endTime = System.currentTimeMillis();
        }
    }

    private void timeCall(final Runnable call, final String callName) {
        timeCall(() -> {
            call.run();
            return null;
        }, callName);
    }
}
