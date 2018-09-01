package com.folkol;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.server.ResourceConfig;

import static org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory.*;

public class Main {
    static final int N_THREADS = 100;
    static final String BASE_URI = "http://localhost:8080/";
    static final int N = 10_000;

    @Path("")
    public static class Resource {
        static volatile int[] ids = new int[N];

        @PUT
        public void put(@QueryParam("id") int id) {
            if (ids[id]++ != 0) {
                System.out.println("Duplicate request: " + id);
            }
        }

        @GET
        public String get() {
            return "Hello, world!";  // The get method needs to produce an entity, which one doesn't matter.
        }
    }

    public static void main(String[] args) throws Exception {
        ResourceConfig rc = new ResourceConfig().register(new Resource());
//        rc.property("jersey.config.server.tracing.type", "ALL");
//        rc.property("jersey.config.server.tracing.threshold", "VERBOSE");
        HttpServer server = createHttpServer(URI.create(BASE_URI), rc);

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(BASE_URI);

        ExecutorService es = Executors.newFixedThreadPool(N_THREADS);
        for (int i = 0; i < N; i++) {
            int id = i;
            es.submit(() -> {
                Response response = target.queryParam("id", id).request().put(Entity.json(""));
                response.close();
            });
            es.submit(() -> {
                Response response = target.request().get();

                // Either of these two lines seems to prevent the double requests.
                // response.close();
                // response.readEntity(String.class);
            });
        }

        es.shutdown();
        es.awaitTermination(1, TimeUnit.MINUTES);
        server.shutdown();
    }
}
