package io.github.delokoseni.model;

import lombok.Getter;

@Getter
public class Cpu {

    private CpuState state;

    private Task currentTask;

    public Cpu() {

        this.state = CpuState.IDLE;

        this.currentTask = null;
    }

    /**
     * Назначает задачу на выполнение.
     */
    public void useToDoTask(Task task) {

        task.setState(
                TaskState.RUNNING
        );

        currentTask = task;
    }

    /**
     * Завершает выполнение задачи CPU.
     */
    public void doTask(Task task) {

        task.setState(
                TaskState.READY
        );

        currentTask = null;
    }

    /**
     * Проверяет, свободен ли CPU.
     */
    public boolean isFree() {

        return currentTask == null;
    }

    /**
     * Устанавливает состояние CPU.
     */
    public void setState(
            CpuState state
    ) {

        this.state = state;
    }

    /**
     * Устанавливает текущую задачу CPU.
     */
    public void setCurrentTask(
            Task task
    ) {

        this.currentTask = task;
    }
}