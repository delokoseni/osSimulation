package io.github.delokoseni.simulation;

import io.github.delokoseni.model.Task;
import io.github.delokoseni.model.TaskPackage;
import io.github.delokoseni.model.TaskType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TaskGenerator {

    private final Random random = new Random();

    public TaskPackage generate(
            int taskCount,
            double cpuBoundRatio,
            double ioBoundRatio,
            double balancedRatio
    ) {
        List<Task> tasks = new ArrayList<>();

        for (int i = 0; i < taskCount; i++) {
            TaskType type = generateType(
                    cpuBoundRatio,
                    ioBoundRatio,
                    balancedRatio
            );

            int memory = generateMemory(type);

            Task task = new Task(
                    i + 1,
                    type,
                    memory,
                    0
            );

            tasks.add(task);
        }

        return new TaskPackage(tasks);
    }

    private TaskType generateType(
            double cpuBoundRatio,
            double ioBoundRatio,
            double balancedRatio
    ) {
        return TaskType.CPU_BOUND;
    }

    private int generateMemory(TaskType type) {
        return 1;
    }
}