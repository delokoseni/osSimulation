package io.github.delokoseni.simulation;

import io.github.delokoseni.model.CpuState;
import io.github.delokoseni.model.TaskState;
import io.github.delokoseni.model.TaskType;

import java.util.List;

public record SimulationSnapshot(
        int tact,
        CpuState cpuState,
        int usedBlocks,
        int maxBlocksCount,
        int mathTasksRunning,
        int ioTasksRunning,
        int completedTasks,
        int waitingTasks,
        int runningTasks,
        List<TaskProgress> taskProgress
) {
    public SimulationSnapshot {
        taskProgress = List.copyOf(taskProgress);
    }

    public record TaskProgress(
            int id,
            TaskType type,
            TaskState state,
            int arrivalTime
    ) {
    }
}
