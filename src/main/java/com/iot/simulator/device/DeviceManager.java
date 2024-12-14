package com.iot.simulator.device;

import com.iot.simulator.grpc.Device;
import com.iot.simulator.grpc.DeviceStatus;
import com.iot.simulator.grpc.StatusUpdate;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class DeviceManager {
    private final Map<String, DeviceContext> deviceContexts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService monitoringExecutor = Executors.newScheduledThreadPool(2);
    private final Map<String, ScheduledFuture<?>> monitoringTasks = new ConcurrentHashMap<>();

    public void registerDevice(Device device) {
        DeviceContext context = new DeviceContext(device);
        deviceContexts.put(device.getDeviceId(), context);
        log.info("Device registered in manager: {}", device.getDeviceId());
    }

    public void startMonitoring(String deviceId, StreamObserver<StatusUpdate> responseObserver) {
        DeviceContext context = deviceContexts.get(deviceId);
        if (context == null) {
            throw new IllegalStateException("Device not found: " + deviceId);
        }

        // Cancel any existing monitoring task
        stopMonitoring(deviceId);

        // Create new monitoring task
        ScheduledFuture<?> monitoringTask = monitoringExecutor.scheduleAtFixedRate(
            () -> sendStatusUpdate(context, responseObserver),
            0, 5, TimeUnit.SECONDS
        );

        monitoringTasks.put(deviceId, monitoringTask);
        context.setMonitoring(true);
        log.info("Started monitoring device: {}", deviceId);
    }

    public void stopMonitoring(String deviceId) {
        ScheduledFuture<?> task = monitoringTasks.remove(deviceId);
        if (task != null) {
            task.cancel(false);
            DeviceContext context = deviceContexts.get(deviceId);
            if (context != null) {
                context.setMonitoring(false);
            }
            log.info("Stopped monitoring device: {}", deviceId);
        }
    }

    private void sendStatusUpdate(DeviceContext context, StreamObserver<StatusUpdate> observer) {
        try {
            DeviceHealth health = context.checkHealth();
            DeviceStatus status = determineStatus(health);
            
            if (status != context.getCurrentStatus()) {
                context.updateStatus(status);
            }

            StatusUpdate update = StatusUpdate.newBuilder()
                .setDeviceId(context.getDevice().getDeviceId())
                .setStatus(status)
                .setMessage(health.getMessage())
                .setTimestamp(System.currentTimeMillis())
                .build();

            observer.onNext(update);
            log.debug("Sent status update for device: {}", context.getDevice().getDeviceId());
        } catch (Exception e) {
            log.error("Error sending status update for device: {}", context.getDevice().getDeviceId(), e);
            observer.onError(e);
        }
    }

    private DeviceStatus determineStatus(DeviceHealth health) {
        if (!health.isReachable()) {
            return DeviceStatus.OFFLINE;
        }
        if (health.getCpuUsage() > 90 || health.getMemoryUsage() > 90) {
            return DeviceStatus.ERROR;
        }
        if (health.isNeedsMaintenance()) {
            return DeviceStatus.MAINTENANCE;
        }
        return DeviceStatus.ONLINE;
    }

    public void shutdown() {
        monitoringExecutor.shutdown();
        try {
            if (!monitoringExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                monitoringExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            monitoringExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}