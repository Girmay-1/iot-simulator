package com.iot.simulator.server;

import com.iot.simulator.grpc.*;
import io.grpc.internal.testing.StreamRecorder;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class IoTServiceImplTest {
    private IoTServiceImpl service;

    @Before
    public void setUp() {
        service = new IoTServiceImpl();
    }

    @Test
    public void testRegisterDevice() throws Exception {
        RegistrationRequest request = RegistrationRequest.newBuilder()
                .setName("TestDevice")
                .setType("Temperature")
                .build();

        StreamRecorder<RegistrationResponse> responseObserver = StreamRecorder.create();
        service.registerDevice(request, responseObserver);

        if (!responseObserver.awaitCompletion(5, TimeUnit.SECONDS)) {
            fail("The call did not terminate in time");
        }

        assertNull(responseObserver.getError());
        List<RegistrationResponse> results = responseObserver.getValues();
        assertEquals(1, results.size());

        RegistrationResponse response = results.get(0);
        assertTrue("Device ID should not be empty", !response.getDeviceId().isEmpty());
        assertTrue("Registration should be successful", response.getSuccess());
    }

    @Test
    public void testRegisterDeviceWithEmptyName() throws Exception {
        RegistrationRequest request = RegistrationRequest.newBuilder()
                .setType("Temperature")
                .build();

        StreamRecorder<RegistrationResponse> responseObserver = StreamRecorder.create();
        service.registerDevice(request, responseObserver);

        if (!responseObserver.awaitCompletion(5, TimeUnit.SECONDS)) {
            fail("The call did not terminate in time");
        }

        assertNull(responseObserver.getError());
        List<RegistrationResponse> results = responseObserver.getValues();
        assertEquals(1, results.size());

        RegistrationResponse response = results.get(0);
        assertTrue("Device ID should be generated even with empty name", !response.getDeviceId().isEmpty());
    }

    @Test
    public void testStreamSensorData() throws Exception {
        StreamRecorder<CommandResponse> responseObserver = StreamRecorder.create();
        
        StreamObserverTestHelper<SensorData> requestObserver = 
            new StreamObserverTestHelper<>(service.streamSensorData(responseObserver));

        SensorData sensorData = SensorData.newBuilder()
                .setDeviceId("test-device-1")
                .setSensorType("temperature")
                .setValue(25.5)
                .setTimestamp(System.currentTimeMillis())
                .build();

        requestObserver.sendData(sensorData);
        requestObserver.complete();

        if (!responseObserver.awaitCompletion(5, TimeUnit.SECONDS)) {
            fail("The call did not terminate in time");
        }

        assertNull(responseObserver.getError());
        List<CommandResponse> results = responseObserver.getValues();
        assertEquals(1, results.size());

        CommandResponse response = results.get(0);
        assertTrue("Sensor data stream should be successful", response.getSuccess());
    }

    @Test
    public void testMonitorDevice() throws Exception {
        String deviceId = registerTestDevice();
        
        SensorDataRequest request = SensorDataRequest.newBuilder()
                .setDeviceId(deviceId)
                .build();

        StreamRecorder<StatusUpdate> responseObserver = StreamRecorder.create();
        service.monitorDevice(request, responseObserver);

        Thread.sleep(1000); // Wait for initial status

        List<StatusUpdate> updates = responseObserver.getValues();
        assertFalse("Should receive status updates", updates.isEmpty());
        
        StatusUpdate update = updates.get(0);
        assertEquals("Device ID should match", deviceId, update.getDeviceId());
        assertNotNull("Status should not be null", update.getStatus());
    }

    @Test
    public void testControlDevice() throws Exception {
        StreamRecorder<CommandResponse> responseObserver = StreamRecorder.create();
        StreamObserverTestHelper<Command> requestObserver = 
            new StreamObserverTestHelper<>(service.controlDevice(responseObserver));

        Command command = Command.newBuilder()
                .setDeviceId("test-device-1")
                .setCommandType("RESET")
                .build();

        requestObserver.sendData(command);
        requestObserver.complete();

        if (!responseObserver.awaitCompletion(5, TimeUnit.SECONDS)) {
            fail("The call did not terminate in time");
        }

        List<CommandResponse> responses = responseObserver.getValues();
        assertFalse("Should receive command responses", responses.isEmpty());
        
        CommandResponse response = responses.get(0);
        assertTrue("Command execution should be successful", response.getSuccess());
        assertEquals("Device ID should match", "test-device-1", response.getDeviceId());
    }

    @Test
    public void testStreamSensorDataWithInvalidValues() throws Exception {
        StreamRecorder<CommandResponse> responseObserver = StreamRecorder.create();
        StreamObserverTestHelper<SensorData> requestObserver = 
            new StreamObserverTestHelper<>(service.streamSensorData(responseObserver));

        // Test with extreme value
        SensorData extremeData = SensorData.newBuilder()
                .setDeviceId("test-device-1")
                .setSensorType("temperature")
                .setValue(Double.MAX_VALUE)
                .setTimestamp(System.currentTimeMillis())
                .build();

        requestObserver.sendData(extremeData);

        // Test with empty device ID
        SensorData emptyDeviceData = SensorData.newBuilder()
                .setSensorType("temperature")
                .setValue(25.5)
                .setTimestamp(System.currentTimeMillis())
                .build();

        requestObserver.sendData(emptyDeviceData);
        requestObserver.complete();

        responseObserver.awaitCompletion(5, TimeUnit.SECONDS);
        assertNull("Should handle invalid data without errors", responseObserver.getError());
    }

    private String registerTestDevice() throws Exception {
        RegistrationRequest request = RegistrationRequest.newBuilder()
                .setName("TestDevice")
                .setType("Temperature")
                .build();

        StreamRecorder<RegistrationResponse> responseObserver = StreamRecorder.create();
        service.registerDevice(request, responseObserver);
        responseObserver.awaitCompletion(5, TimeUnit.SECONDS);
        
        return responseObserver.getValues().get(0).getDeviceId();
    }
}

class StreamObserverTestHelper<T> {
    private io.grpc.stub.StreamObserver<T> streamObserver;

    StreamObserverTestHelper(io.grpc.stub.StreamObserver<T> streamObserver) {
        this.streamObserver = streamObserver;
    }

    void sendData(T data) {
        streamObserver.onNext(data);
    }

    void complete() {
        streamObserver.onCompleted();
    }

    void error(Throwable t) {
        streamObserver.onError(t);
    }
}