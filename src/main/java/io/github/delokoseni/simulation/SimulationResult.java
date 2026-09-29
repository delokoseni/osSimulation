package io.github.delokoseni.simulation;

import lombok.Getter;

@Getter
public class SimulationResult {

    private final int totalTime;

    private final int completedTasks;

    private final double throughput;

    private final double averageTurnaroundTime;

    private final double cpuIdlePercentage;

    public SimulationResult(
            int totalTime,
            int completedTasks,
            double throughput,
            double averageTurnaroundTime,
            double cpuIdlePercentage
    ) {
        this.totalTime = totalTime;
        this.completedTasks = completedTasks;
        this.throughput = throughput;
        this.averageTurnaroundTime = averageTurnaroundTime;
        this.cpuIdlePercentage = cpuIdlePercentage;
    }
}