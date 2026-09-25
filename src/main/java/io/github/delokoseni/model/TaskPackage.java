package io.github.delokoseni.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class TaskPackage {

    private final List<Task> tasks;

    public TaskPackage(List<Task> tasks) {
        this.tasks = new ArrayList<>(tasks);
    }

    public int getTaskCount() {
        return tasks.size();
    }

    public boolean isEmpty() {
        return tasks.isEmpty();
    }

    /**
     * Количество вычислительных задач.
     */
    public int getMathTasks() {

        return (int) tasks.stream()
                .filter(task ->
                        task.getType()
                                == TaskType.CPU_BOUND
                )
                .count();
    }

    /**
     * Количество задач ввода/вывода.
     */
    public int getInOutTasks() {

        return (int) tasks.stream()
                .filter(task ->
                        task.getType()
                                == TaskType.IO_BOUND
                )
                .count();
    }

    /**
     * Суммарный объем памяти всех задач.
     *
     * Память задач хранится в MB.
     */
    public int getTasksMemory() {

        return tasks.stream()
                .mapToInt(Task::getMemoryRequired)
                .sum();
    }
}