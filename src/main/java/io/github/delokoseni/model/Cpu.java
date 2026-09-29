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

    public void useToDoTask(Task task) {
        task.setState(
                TaskState.RUNNING
        );

        currentTask = task;
    }

    public void doTask(Task task) {
        task.setState(
                TaskState.READY
        );

        currentTask = null;
    }

    public boolean isFree() {
        return currentTask == null;
    }

    public void setState(
            CpuState state
    ) {
        this.state = state;
    }

    public void setCurrentTask(
            Task task
    ) {
        this.currentTask = task;
    }
}