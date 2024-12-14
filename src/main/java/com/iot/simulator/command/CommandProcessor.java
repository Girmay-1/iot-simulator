package com.iot.simulator.command;

import com.iot.simulator.grpc.Command;
import com.iot.simulator.grpc.CommandResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;

@Slf4j
public class CommandProcessor {
    
    private static final Set<String> VALID_COMMANDS = Set.of(
        "RESTART",
        "CALIBRATE",
        "UPDATE_INTERVAL",
        "SLEEP",
        "WAKE",
        "SET_CONFIG"
    );

    public CommandResponse processCommand(Command command) {
        if (!isValidCommand(command)) {
            return CommandResponse.newBuilder()
                .setDeviceId(command.getDeviceId())
                .setSuccess(false)
                .setMessage("Invalid command or parameters")
                .build();
        }

        try {
            return executeCommand(command);
        } catch (Exception e) {
            log.error("Error executing command: {}", command.getCommandType(), e);
            return CommandResponse.newBuilder()
                .setDeviceId(command.getDeviceId())
                .setSuccess(false)
                .setMessage("Command execution failed: " + e.getMessage())
                .build();
        }
    }

    private boolean isValidCommand(Command command) {
        if (command == null || command.getDeviceId().isEmpty()) {
            return false;
        }

        if (!VALID_COMMANDS.contains(command.getCommandType().toUpperCase())) {
            return false;
        }

        return validateCommandParameters(command);
    }

    private boolean validateCommandParameters(Command command) {
        Map<String, String> params = command.getParametersMap();
        
        return switch (command.getCommandType().toUpperCase()) {
            case "UPDATE_INTERVAL" -> validateUpdateIntervalParams(params);
            case "SET_CONFIG" -> validateSetConfigParams(params);
            case "CALIBRATE" -> validateCalibrateParams(params);
            // RESTART, SLEEP, and WAKE don't require parameters
            default -> true;
        };
    }

    private boolean validateUpdateIntervalParams(Map<String, String> params) {
        try {
            String interval = params.get("interval");
            if (interval == null) return false;
            int value = Integer.parseInt(interval);
            return value > 0 && value <= 3600; // 1 hour max
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean validateSetConfigParams(Map<String, String> params) {
        return params.containsKey("key") && params.containsKey("value");
    }

    private boolean validateCalibrateParams(Map<String, String> params) {
        return params.containsKey("sensor_type");
    }

    private CommandResponse executeCommand(Command command) {
        // Simulate command execution with appropriate delays
        String commandType = command.getCommandType().toUpperCase();
        
        // Simulate command execution time
        simulateExecutionTime(commandType);
        
        String resultMessage = switch (commandType) {
            case "RESTART" -> executeRestart();
            case "CALIBRATE" -> executeCalibrate(command.getParametersMap());
            case "UPDATE_INTERVAL" -> executeUpdateInterval(command.getParametersMap());
            case "SLEEP" -> executeSleep();
            case "WAKE" -> executeWake();
            case "SET_CONFIG" -> executeSetConfig(command.getParametersMap());
            default -> throw new IllegalStateException("Unexpected command: " + commandType);
        };

        return CommandResponse.newBuilder()
            .setDeviceId(command.getDeviceId())
            .setSuccess(true)
            .setMessage(resultMessage)
            .build();
    }

    private void simulateExecutionTime(String commandType) {
        try {
            long delay = switch (commandType) {
                case "RESTART" -> 2000; // 2 seconds for restart
                case "CALIBRATE" -> 5000; // 5 seconds for calibration
                case "UPDATE_INTERVAL", "SET_CONFIG" -> 500; // 0.5 seconds for configuration
                default -> 1000; // 1 second for other commands
            };
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Command execution interrupted", e);
        }
    }

    private String executeRestart() {
        log.info("Executing restart command");
        return "Device restarted successfully";
    }

    private String executeCalibrate(Map<String, String> params) {
        String sensorType = params.get("sensor_type");
        log.info("Calibrating sensor: {}", sensorType);
        return "Calibration completed for " + sensorType;
    }

    private String executeUpdateInterval(Map<String, String> params) {
        String interval = params.get("interval");
        log.info("Updating sampling interval to: {}", interval);
        return "Sampling interval updated to " + interval + " seconds";
    }

    private String executeSleep() {
        log.info("Executing sleep command");
        return "Device entered sleep mode";
    }

    private String executeWake() {
        log.info("Executing wake command");
        return "Device woken up";
    }

    private String executeSetConfig(Map<String, String> params) {
        String key = params.get("key");
        String value = params.get("value");
        log.info("Setting config {}={}", key, value);
        return "Configuration updated: " + key + "=" + value;
    }
}