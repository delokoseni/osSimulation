package io.github.delokoseni.model;

import lombok.Getter;

@Getter
public class Cpu {

    private CpuState state;

    private Task currentTask;

    /**
     * Количество тактов, в течение которых CPU простаивал.
     */
    private int idleTime;

    /**
     * Общее количество обработанных тактов.
     */
    private int busyTime;

    public Cpu() {
        this.state = CpuState.IDLE;
    }

    public boolean isFree() {
        return currentTask == null;
    }

    public void assignTask(Task task) {
        // TODO: реализовать назначение задачи процессору
    }

    public void executeTick() {
        // TODO: реализовать выполнение одного такта
    }

    public void releaseTask() {
        // TODO: освободить CPU
    }

    public void incrementIdleTime() {
        idleTime++;
    }

    public void incrementBusyTime() {
        busyTime++;
    }
}