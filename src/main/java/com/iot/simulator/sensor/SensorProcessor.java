package com.iot.simulator.sensor;

import com.iot.simulator.grpc.SensorData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SensorProcessor {
    
    public boolean processSensorData(SensorData data) {
        try {
            // Validate basic requirements
            if (!isValid(data)) {
                log.error("Invalid sensor data received: {}", data);
                return false;
            }

            // Process the data (for now just log it)
            log.info("Processing sensor data - Device: {}, Type: {}, Value: {}, Time: {}", 
                data.getDeviceId(), 
                data.getSensorType(), 
                data.getValue(), 
                data.getTimestamp());

            // TODO: When we add the data tier tomorrow, we'll store the data here
            
            return true;
        } catch (Exception e) {
            log.error("Error processing sensor data", e);
            return false;
        }
    }

    private boolean isValid(SensorData data) {
        // Basic validation
        if (data == null) return false;
        if (data.getDeviceId().isEmpty()) return false;
        if (data.getSensorType().isEmpty()) return false;
        if (data.getTimestamp() <= 0) return false;
        
        // Validate value ranges based on sensor type
        return switch (data.getSensorType().toUpperCase()) {
            case "TEMPERATURE" -> isValidTemperature(data.getValue());
            case "HUMIDITY" -> isValidHumidity(data.getValue());
            case "PRESSURE" -> isValidPressure(data.getValue());
            default -> false;
        };
    }

    private boolean isValidTemperature(double value) {
        return value >= -40.0 && value <= 125.0;  // Standard temperature sensor range
    }

    private boolean isValidHumidity(double value) {
        return value >= 0.0 && value <= 100.0;    // Humidity percentage
    }

    private boolean isValidPressure(double value) {
        return value >= 800.0 && value <= 1200.0; // Standard atmospheric pressure range (hPa)
    }
}