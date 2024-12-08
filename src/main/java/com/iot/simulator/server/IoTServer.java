package com.iot.simulator.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class IoTServer {
    private final Server server;
    private final int port;

    public IoTServer(int port) {
        this.port = port;
        this.server = ServerBuilder.forPort(port)
                .addService(new IoTServiceImpl())
                .build();
    }

    public void start() throws IOException {
        server.start();
        log.info("IoT Server started on port: {}", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down IoT server due to JVM shutdown");
            IoTServer.this.stop();
            log.info("IoT server shut down successfully");
        }));
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = 50051; // Default gRPC port
        IoTServer server = new IoTServer(port);
        server.start();
        server.blockUntilShutdown();
    }
}