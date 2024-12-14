package com.iot.simulator.server;

import com.iot.simulator.grpc.*;
import com.iot.simulator.device.DeviceManager;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class IoTServiceImpl extends IoTServiceGrpc.IoTServiceImplBase {
    
    private final Map<String, Device> devices = new ConcurrentHashMap<>();
    private final DeviceManager deviceManager = new DeviceManager();

    @Override
    public void registerDevice(RegistrationRequest request, StreamObserver<RegistrationResponse> responseObserver) {
        log.info("Received registration request for device: {}", request.getName());
        
        String deviceId = UUID.randomUUID().toString();
        
        Device device = Device.newBuilder()
                .setDeviceId(deviceId)
                .setName(request.getName())
                .setType(request.getType())
                .setStatus(DeviceStatus.ONLINE)
                .build();
        
        devices.put(deviceId, device);
        deviceManager.registerDevice(device);
        
        RegistrationResponse response = RegistrationResponse.newBuilder()
                .setDeviceId(deviceId)
                .setSuccess(true)
                .setMessage("Device registered successfully")
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        
        log.info("Device registered with ID: {}", deviceId);
    }

    @Override
    public StreamObserver<SensorData> streamSensorData(StreamObserver<CommandResponse> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(SensorData sensorData) {
                log.info("Received sensor data from device {}: {} = {}", 
                    sensorData.getDeviceId(), 
                    sensorData.getSensorType(), 
                    sensorData.getValue());
                
                // Process the sensor data
                // TODO: Implement data processing logic
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error in sensor data stream", t);
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                CommandResponse response = CommandResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Sensor data stream processed successfully")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                log.info("Sensor data stream completed");
            }
        };
    }

    @Override
    public void monitorDevice(SensorDataRequest request, StreamObserver<StatusUpdate> responseObserver) {
        String deviceId = request.getDeviceId();
        log.info("Starting monitoring for device: {}", deviceId);
        
        if (!devices.containsKey(deviceId)) {
            responseObserver.onError(
                new IllegalArgumentException("Device not found: " + deviceId)
            );
            return;
        }

        try {
            deviceManager.startMonitoring(deviceId, responseObserver);
        } catch (Exception e) {
            log.error("Error starting device monitoring", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public StreamObserver<Command> controlDevice(StreamObserver<CommandResponse> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(Command command) {
                log.info("Received command for device {}: {}", 
                    command.getDeviceId(), 
                    command.getCommandType());
                
                // TODO: Implement command execution logic
                CommandResponse response = CommandResponse.newBuilder()
                        .setDeviceId(command.getDeviceId())
                        .setSuccess(true)
                        .setMessage("Command processed: " + command.getCommandType())
                        .build();
                
                responseObserver.onNext(response);
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error in command stream", t);
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
                log.info("Command stream completed");
            }
        };
    }

    @PreDestroy
    public void shutdown() {
        deviceManager.shutdown();
    }
}