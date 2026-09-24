package io.github.delokoseni.simulation;

import io.github.delokoseni.model.Cpu;
import io.github.delokoseni.model.Task;
import io.github.delokoseni.model.TaskPackage;
import lombok.Getter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

@Getter
public class OperatingSystem {

    /**
     * Процессор.
     */
    private final Cpu cpu;

    /**
     * Очередь готовых к выполнению задач.
     */
    private final Queue<Task> readyQueue;

    /**
     * Задачи, ожидающие завершения операции ввода-вывода.
     */
    private final List<Task> waitingTasks;

    /**
     * Новые задачи, еще не загруженные в память.
     */
    private final Queue<Task> newTasks;

    /**
     * Завершенные задачи.
     */
    private final List<Task> completedTasks;

    /**
     * Максимальный объем доступной памяти.
     */
    private final int memoryCapacity;

    /**
     * Текущий объем занятой памяти.
     */
    private int usedMemory;

    /**
     * Текущий такт моделирования.
     */
    private int currentTime;

    public OperatingSystem(
            TaskPackage taskPackage,
            int memoryCapacity
    ) {
        this.cpu = new Cpu();

        this.readyQueue = new ArrayDeque<>();
        this.waitingTasks = new ArrayList<>();
        this.newTasks = new ArrayDeque<>(taskPackage.getTasks());
        this.completedTasks = new ArrayList<>();

        this.memoryCapacity = memoryCapacity;
        this.usedMemory = 0;
        this.currentTime = 0;
    }

    /**
     * Выполняет один такт работы операционной системы.
     */
    public void tick() {
        // TODO:
        // 1. Обработать задачи, ожидающие I/O
        // 2. Загрузить новые задачи в память
        // 3. Назначить задачу CPU
        // 4. Выполнить один такт CPU
        // 5. Проверить завершение задач
        // 6. currentTime++
    }

    private void loadTasks() {
        // TODO: загрузка задач из newTasks в память
    }

    private void processWaitingTasks() {
        // TODO: обработка I/O
    }

    private void schedule() {
        // TODO: выбор следующей задачи из readyQueue
    }

    private void finishTask(Task task) {
        // TODO: освобождение памяти и перенос задачи
        // в completedTasks
    }

    public boolean isFinished() {
        return newTasks.isEmpty()
                && readyQueue.isEmpty()
                && waitingTasks.isEmpty()
                && cpu.isFree();
    }
}