package io.github.delokoseni.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TaskPackageType {

    public enum TypePacket {
        MATH_PACK("ВЫЧИСЛИТЕЛЬНЫЙ"),
        INOUT_PACK("ВВОД/ВЫВОД"),
        BALANCED_PACK("СБАЛАНСИРОВАННЫЙ");

        private final String displayName;

        TypePacket(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final List<Task> tasks;
    private TypePacket type;

    public TaskPackageType() {
        this(List.of());
    }

    public TaskPackageType(List<Task> tasks) {
        this.tasks = new ArrayList<>(tasks);
        this.type = this.tasks.isEmpty() ? null : checkPacketType();
    }

    public TaskPackageType(String filename) throws IOException {
        this(createByJson(filename));
    }

    public TaskPackageType(Path path) throws IOException {
        this(createByJson(path));
    }

    public TypePacket checkPacketType() {
        int mathTasks = getMathTasks();
        int inOutTasks = getInOutTasks();

        if (mathTasks == inOutTasks) {
            return TypePacket.BALANCED_PACK;
        }
        return mathTasks > inOutTasks
                ? TypePacket.MATH_PACK
                : TypePacket.INOUT_PACK;
    }

    public int getTasksCount() {
        return tasks.size();
    }

    public int getTasksMemory() {
        return tasks.stream()
                .mapToInt(Task::getMemoryRequired)
                .sum();
    }

    public int getWaitTasks() {
        return countTasksInState(TaskState.WAITING);
    }

    public int getRunTasks() {
        return countTasksInState(TaskState.RUNNING);
    }

    public int getReadyTasks() {
        return countTasksInState(TaskState.READY);
    }

    public int getMathTasks() {
        return (int) tasks.stream()
                .filter(task -> task.getType() == TaskType.CPU_BOUND)
                .count();
    }

    public int getInOutTasks() {
        return (int) tasks.stream()
                .filter(task -> task.getType() == TaskType.IO_BOUND)
                .count();
    }

    public List<Task> getTasks() {
        return List.copyOf(tasks);
    }

    public TypePacket getType() {
        return type;
    }

    private int countTasksInState(TaskState state) {
        return (int) tasks.stream()
                .filter(task -> task.getState() == state)
                .count();
    }

    public static List<Task> createByJson(String filename) throws IOException {
        return createByJson(Path.of(filename));
    }

    public static List<Task> createByJson(Path path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(path.toFile());
        JsonNode taskNodes = root.path("tasks");

        if (!taskNodes.isArray()) {
            throw new IOException("JSON packet must contain a tasks array: " + path);
        }

        List<Task> tasks = new ArrayList<>();
        for (JsonNode taskNode : taskNodes) {
            tasks.add(new Task(
                    taskNode.path("num").asInt(),
                    parseTaskType(taskNode.path("type").asText()),
                    taskNode.path("memory").asInt()
            ));
        }
        return tasks;
    }

    private static TaskType parseTaskType(String type) throws IOException {
        return switch (type) {
            case "MATH", "CPU_BOUND" -> TaskType.CPU_BOUND;
            case "INOUT", "IO_BOUND" -> TaskType.IO_BOUND;
            default -> throw new IOException("Unknown task type: " + type);
        };
    }
}
