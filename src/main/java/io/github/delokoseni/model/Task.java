package io.github.delokoseni.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Task {

    private int id;

    private final TaskType type;

    private final int memoryRequired;

    private final int arrivalTime;

    private TaskState state;

    private int executionTime;

    private final int requiredTime;

    private int remainingCpuTime;

    private int remainingIoTime;

    private int completionTime;

    public Task(
            int id,
            TaskType type,
            int memoryRequired,
            int arrivalTime
    ) {
        this.id = id;
        this.type = type;
        this.memoryRequired = memoryRequired;
        this.arrivalTime = arrivalTime;

        this.state = TaskState.WAITING;

        this.executionTime = 0;

        this.requiredTime =
                type == TaskType.CPU_BOUND ? 3 : 2;

        this.remainingCpuTime = 0;
        this.remainingIoTime = 0;
        this.completionTime = 0;
    }

    public boolean execute() {
        if (state == TaskState.RUNNING) {
            executionTime++;

            if (executionTime >= requiredTime) {
                state = TaskState.READY;
                return true;
            }
        }

        return false;
    }
}