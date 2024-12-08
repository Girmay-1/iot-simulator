package com.iot.simulator.client;

import com.iot.simulator.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class IoTClient {
    private final ManagedChannel channel;
    private final IoTServiceGrpc.IoTServiceBlockingStub blockingStub;
    private final IoTServiceGrpc.IoTServiceStub asyncStub;

    public IoTClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext() // Don't use this in production without proper security
                .build();
        blockingStub = IoTServiceGrpc.newBlockingStub(channel);
        asyncStub = IoTServiceGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public String registerDevice(String name, String type) {
        RegistrationRequest request = RegistrationRequest.newBuilder()
                .setName(name)
                .setType(type)
                .build();

        RegistrationResponse response = blockingStub.registerDevice(request);
        log.info("Device registration response: {}", response.getMessage());
        return response.getDeviceId();
    }

    public void streamSensorData(String deviceId) throws InterruptedException {
        CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<CommandResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(CommandResponse response) {
                log.info("Received command response: {}", response.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error in sensor data streaming", t);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                log.info("Completed sensor data streaming");
                finishLatch.countDown();
            }
        };

        StreamObserver<SensorData> requestObserver = asyncStub.streamSensorData(responseObserver);

        try {
            // Simulate sending sensor data
            for (int i = 0; i < 5; i++) {
                SensorData sensorData = SensorData.newBuilder()
                        .setDeviceId(deviceId)
                        .setSensorType("temperature")
                        .setValue(20 + Math.random() * 10)
                        .setTimestamp(System.currentTimeMillis())
                        .build();

                requestObserver.onNext(sensorData);
                Thread.sleep(1000);
            }
        } catch (RuntimeException e) {
            requestObserver.onError(e);
            throw e;
        }

        requestObserver.onCompleted();
        finishLatch.await(1, TimeUnit.MINUTES);
    }

    public static void main(String[] args) throws InterruptedException {
        IoTClient client = new IoTClient("localhost", 50051);
        try {
            // Register a device
            String deviceId = client.registerDevice("Temperature Sensor 1", "TEMPERATURE");
            log.info("Registered device with ID: {}", deviceId);

            // Stream some sensor data
            client.streamSensorData(deviceId);
        } finally {
            client.shutdown();
        }
    }
}