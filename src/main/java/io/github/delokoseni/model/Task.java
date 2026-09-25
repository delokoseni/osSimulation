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
     * Сколько тактов задача уже выполнялась.
     */
    private int executionTime;

    /**
     * Сколько тактов требуется задаче.
     *
     * CPU_BOUND = 3
     * IO_BOUND = 2
     */
    private final int requiredTime;

    /**
     * Оставшееся время CPU-burst.
     */
    private int remainingCpuTime;

    /**
     * Оставшееся время I/O-burst.
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

        // Python StateTask.WAIT
        this.state = TaskState.WAITING;

        this.executionTime = 0;

        // Python:
        // MATH -> 3
        // INOUT -> 2
        this.requiredTime =
                type == TaskType.CPU_BOUND ? 3 : 2;

        this.remainingCpuTime = 0;
        this.remainingIoTime = 0;
        this.completionTime = 0;
    }

    /**
     * Выполнить один такт.
     *
     * Аналог Python Task.execute().
     */
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