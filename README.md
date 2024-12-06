# IoT Device Simulator

A gRPC-based IoT device simulator that demonstrates various patterns of device communication and management.

## Features

### Device Registration and Management
- Register new devices with unique IDs
- Device type and metadata management
- Status tracking (ONLINE, OFFLINE, MAINTENANCE, ERROR)

### Real-time Sensor Data Streaming
- Client-side streaming of sensor data
- Support for different sensor types
- Timestamp and metadata included with readings
- Configurable streaming frequency

### Device Status Monitoring
- Server-side streaming of device status
- Real-time status updates
- Error and maintenance state notifications
- Health monitoring

### Command and Control Interface
- Bidirectional streaming for device control
- Real-time command execution
- Command acknowledgement and response
- Parameter-based command configuration

## Technical Stack
- Java 17
- gRPC
- Protocol Buffers
- Maven
- Logback for logging
- JUnit for testing

## Project Structure

```
iot-simulator/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/iot/simulator/
│   │   │       ├── server/       # gRPC server implementation
│   │   │       ├── client/       # Simulator client implementation
│   │   │       ├── device/       # Device simulation logic
│   │   │       └── util/         # Utility classes
│   │   └── proto/
│   │       └── iot_service.proto # Service definitions
│   └── test/                     # Test cases
└── pom.xml
```

## Getting Started

### Prerequisites
- Java 17 or later
- Maven 3.6 or later

### Building the Project
```bash
mvn clean install
```

### Running the Server
```bash
mvn exec:java -Dexec.mainClass="com.iot.simulator.server.IoTServer"
```

### Running the Client
```bash
mvn exec:java -Dexec.mainClass="com.iot.simulator.client.IoTClient"
```

## Usage Examples

### Registering a Device
```java
RegistrationRequest request = RegistrationRequest.newBuilder()
    .setName("Temperature Sensor 1")
    .setType("TEMPERATURE")
    .build();
    
RegistrationResponse response = stub.registerDevice(request);
```

### Streaming Sensor Data
```java
StreamObserver<SensorData> requestObserver = stub.streamSensorData(responseObserver);
requestObserver.onNext(SensorData.newBuilder()
    .setDeviceId("device-1")
    .setSensorType("TEMPERATURE")
    .setValue(22.5)
    .setTimestamp(System.currentTimeMillis())
    .build());
```

## License
This project is licensed under the MIT License - see the LICENSE file for details.