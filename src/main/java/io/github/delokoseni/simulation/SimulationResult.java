package io.github.delokoseni.simulation;

import lombok.Getter;

@Getter
public class SimulationResult {

    /**
     * Общее время моделирования.
     */
    private final int totalTime;

    /**
     * Количество завершенных задач.
     */
    private final int completedTasks;

    /**
     * Производительность системы.
     */
    private final double throughput;

    /**
     * Среднее время оборота задачи.
     */
    private final double averageTurnaroundTime;

    /**
     * Процент времени простоя CPU.
     */
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