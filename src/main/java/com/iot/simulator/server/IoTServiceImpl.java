package com.iot.simulator.server;

import com.iot.simulator.grpc.*;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class IoTServiceImpl extends IoTServiceGrpc.IoTServiceImplBase {
    
    // Store registered devices and their status
    private final Map<String, Device> devices = new ConcurrentHashMap<>();

    @Override
    public void registerDevice(RegistrationRequest request, StreamObserver<RegistrationResponse> responseObserver) {
        log.info("Received registration request for device: {}", request.getName());
        
        // Generate a unique device ID
        String deviceId = UUID.randomUUID().toString();
        
        // Create and store the device
        Device device = Device.newBuilder()
                .setDeviceId(deviceId)
                .setName(request.getName())
                .setType(request.getType())
                .setStatus(DeviceStatus.ONLINE)
                .build();
        
        devices.put(deviceId, device);
        
        // Send response
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
                // For now, we'll just acknowledge receipt
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
        
        // Here we would typically start a monitoring thread or process
        // For demonstration, we'll just send a single status update
        
        if (devices.containsKey(deviceId)) {
            StatusUpdate update = StatusUpdate.newBuilder()
                    .setDeviceId(deviceId)
                    .setStatus(devices.get(deviceId).getStatus())
                    .setMessage("Device is being monitored")
                    .setTimestamp(System.currentTimeMillis())
                    .build();
            
            responseObserver.onNext(update);
        }
        
        // In a real implementation, you would continue sending updates
        // For now, we'll just complete the stream
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<Command> controlDevice(StreamObserver<CommandResponse> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(Command command) {
                log.info("Received command for device {}: {}", 
                    command.getDeviceId(), 
                    command.getCommandType());
                
                // Process the command and send response
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
}