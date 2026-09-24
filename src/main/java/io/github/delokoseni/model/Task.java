package io.github.delokoseni.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Task {

    private int id;

    private final TaskType type;

    /**
     * Объем памяти, необходимый задаче.
     */
    private final int memoryRequired;

    /**
     * Момент поступления задачи в систему.
     */
    private final int arrivalTime;

    /**
     * Текущее состояние задачи.
     */
    private TaskState state;

    /**
     * Оставшееся время текущего CPU-burst.
     */
    private int remainingCpuTime;

    /**
     * Оставшееся время текущего I/O-burst.
     */
    private int remainingIoTime;

    /**
     * Момент завершения задачи.
     */
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
        this.state = TaskState.NEW;
    }
}