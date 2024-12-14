package com.iot.simulator.device;

import com.iot.simulator.grpc.Device;
import com.iot.simulator.grpc.DeviceStatus;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class DeviceContext {
    @Getter
    private final Device device;
    private final AtomicReference<DeviceStatus> currentStatus;
    private final AtomicBoolean isMonitoring;
    private final Random random;
    private Instant lastMaintenanceCheck;
    private static final long MAINTENANCE_INTERVAL = 24 * 60 * 60 * 1000; // 24 hours

    public DeviceContext(Device device) {
        this.device = device;
        this.currentStatus = new AtomicReference<>(device.getStatus());
        this.isMonitoring = new AtomicBoolean(false);
        this.random = new Random();
        this.lastMaintenanceCheck = Instant.now();
    }

    public DeviceStatus getCurrentStatus() {
        return currentStatus.get();
    }

    public void updateStatus(DeviceStatus status) {
        currentStatus.set(status);
        log.info("Device {} status updated to {}", device.getDeviceId(), status);
    }

    public void setMonitoring(boolean monitoring) {
        isMonitoring.set(monitoring);
    }

    public boolean isMonitoring() {
        return isMonitoring.get();
    }

    public DeviceHealth checkHealth() {
        // Simulate device health check
        boolean reachable = random.nextDouble() > 0.01; // 1% chance of being unreachable
        double cpuUsage = 40 + random.nextDouble() * 60; // Random CPU usage between 40-100%
        double memoryUsage = 30 + random.nextDouble() * 70; // Random memory usage between 30-100%
        

        boolean needsMaintenance = Instant.now().isAfter(
            lastMaintenanceCheck.plusMillis(MAINTENANCE_INTERVAL)
        );

        if (needsMaintenance) {
            lastMaintenanceCheck = Instant.now();
        }

        return DeviceHealth.builder()
            .reachable(reachable)
            .cpuUsage(cpuUsage)
            .memoryUsage(memoryUsage)
            .needsMaintenance(needsMaintenance)
            .message(generateHealthMessage(reachable, cpuUsage, memoryUsage, needsMaintenance))
            .build();
    }

    private String generateHealthMessage(boolean reachable, double cpu, double memory, boolean maintenance) {
        if (!reachable) {
            return "Device is unreachable";
        }
        if (cpu > 90) {
            return "High CPU usage: " + String.format("%.1f%%", cpu);
        }
        if (memory > 90) {
            return "High memory usage: " + String.format("%.1f%%", memory);
        }
        if (maintenance) {
            return "Routine maintenance required";
        }
        return "Device operating normally";
    }
}