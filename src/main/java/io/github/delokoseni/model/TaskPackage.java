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

    public int getMathTasks() {
        return (int) tasks.stream()
                .filter(task ->
                        task.getType()
                                == TaskType.CPU_BOUND
                )
                .count();
    }

    public int getInOutTasks() {
        return (int) tasks.stream()
                .filter(task ->
                        task.getType()
                                == TaskType.IO_BOUND
                )
                .count();
    }

    public int getTasksMemory() {
        return tasks.stream()
                .mapToInt(Task::getMemoryRequired)
                .sum();
    }
}