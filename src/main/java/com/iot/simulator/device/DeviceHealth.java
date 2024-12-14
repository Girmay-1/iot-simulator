package com.iot.simulator.device;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DeviceHealth {
    boolean reachable;
    double cpuUsage;
    double memoryUsage;
    boolean needsMaintenance;
    String message;
}