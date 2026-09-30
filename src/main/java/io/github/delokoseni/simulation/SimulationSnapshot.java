package io.github.delokoseni.simulation;

import io.github.delokoseni.model.TaskState;
import io.github.delokoseni.model.TaskType;

import java.util.List;

public record SimulationSnapshot(
        int tact,
        boolean cpuActive,
        boolean ioActive,
        boolean loadUnloadActive,
        List<TaskProgress> taskProgress
) {
    public SimulationSnapshot {
        taskProgress = List.copyOf(taskProgress);
    }

    public record TaskProgress(
            int id,
            TaskType type,
            TaskState state
    ) {
    }
}
