package io.github.delokoseni.simulation;

import io.github.delokoseni.model.Cpu;
import io.github.delokoseni.model.CpuState;
import io.github.delokoseni.model.Task;
import io.github.delokoseni.model.TaskPackage;
import io.github.delokoseni.model.TaskState;
import io.github.delokoseni.model.TaskType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Getter
public class OperatingSystem {

    /**
     * Объем оперативной памяти в ГБ.
     */
    private final int ram;

    /**
     * Максимальное количество разделов памяти.
     */
    private int maxBlocksCount;

    /**
     * Пакет заданий.
     */
    private final TaskPackage packet;

    /**
     * Разделы памяти.
     *
     * Каждый элемент содержит задачу,
     * находящуюся в соответствующем разделе.
     */
    private List<Task> memoryBlocks;

    /**
     * Очередь ожидающих загрузки задач.
     */
    private List<Task> waitQueue;

    /**
     * Очередь завершенных задач.
     */
    private List<Task> readyQueue;

    /**
     * Выполняющиеся задачи.
     */
    private List<Task> runningTasks;

    /**
     * Задачи, ожидающие завершения I/O.
     */
    private List<Task> ioWaitTasks;

    /**
     * Процессор.
     */
    private final Cpu cpu;

    /**
     * Текущий такт.
     */
    private int currentTact;

    /**
     * Callback для вывода информации в интерфейс.
     */
    private Consumer<String> outputCallback;

    /**
     * История выполнения.
     */
    private Map<String, Object> history;

    /**
     * Счетчики состояний CPU.
     */
    private Map<String, Integer> cpuStateCounts;

    public OperatingSystem(
            TaskPackage packet,
            int ram,
            int maxBlocksCount
    ) {

        if (ram <= 0) {
            throw new IllegalArgumentException(
                    "Объем RAM должен быть положительным"
            );
        }

        if (maxBlocksCount <= 0) {
            throw new IllegalArgumentException(
                    "Количество разделов памяти должно быть положительным"
            );
        }

        this.ram = ram;
        this.maxBlocksCount = maxBlocksCount;
        this.packet = packet;

        this.memoryBlocks = new ArrayList<>(
                maxBlocksCount
        );

        for (int i = 0; i < maxBlocksCount; i++) {
            memoryBlocks.add(null);
        }

        this.waitQueue = new ArrayList<>(
                packet.getTasks()
        );

        this.readyQueue = new ArrayList<>();

        this.runningTasks = new ArrayList<>();

        this.ioWaitTasks = new ArrayList<>();

        this.cpu = new Cpu();

        this.currentTact = 0;

        initializeHistory();
        initializeCpuStateCounts();
    }

    // =========================================================
    // ИНИЦИАЛИЗАЦИЯ
    // =========================================================

    /**
     * Инициализация истории выполнения.
     */
    private void initializeHistory() {

        history = new HashMap<>();

        history.put(
                "tacts",
                new ArrayList<Integer>()
        );

        history.put(
                "memory_blocks_used",
                new ArrayList<Integer>()
        );

        history.put(
                "cpu_states",
                new ArrayList<String>()
        );

        Map<String, List<Integer>> taskStates =
                new HashMap<>();

        taskStates.put(
                "WAIT",
                new ArrayList<>()
        );

        taskStates.put(
                "RUN",
                new ArrayList<>()
        );

        taskStates.put(
                "READY",
                new ArrayList<>()
        );

        history.put(
                "task_states",
                taskStates
        );

        history.put(
                "memory_usage",
                new ArrayList<Double>()
        );

        Map<String, Integer> taskTypes =
                new HashMap<>();

        taskTypes.put(
                "MATH",
                packet.getMathTasks()
        );

        taskTypes.put(
                "INOUT",
                packet.getInOutTasks()
        );

        history.put(
                "task_types",
                taskTypes
        );
    }

    /**
     * Инициализация счетчиков состояний CPU.
     */
    private void initializeCpuStateCounts() {

        cpuStateCounts = new HashMap<>();

        cpuStateCounts.put(
                "ПРОСТОЙ",
                0
        );

        cpuStateCounts.put(
                "ВЫПОЛНЕНИЕ ВЫЧИСЛЕНИЙ",
                0
        );

        cpuStateCounts.put(
                "ОЖИДАНИЕ ЗАВЕРШЕНИЯ ВВОДА/ВЫВОДА",
                0
        );

        cpuStateCounts.put(
                "ПЕРЕГРУЗКА",
                0
        );
    }

    // =========================================================
    // РАБОТА С ПАМЯТЬЮ
    // =========================================================

    /**
     * Изменение количества разделов памяти.
     *
     * Аналог Python changeMemoryBlocksCount().
     */
    public void changeMemoryBlocksCount(
            int newCount
    ) {

        if (newCount <= 0) {
            throw new IllegalArgumentException(
                    "Количество разделов памяти должно быть положительным"
            );
        }

        int oldCount = maxBlocksCount;

        maxBlocksCount = newCount;

        /*
         * Получаем все задачи,
         * которые сейчас находятся в памяти.
         */
        List<Task> currentTasks =
                new ArrayList<>();

        for (Task task : memoryBlocks) {

            if (task != null) {
                currentTasks.add(task);
            }
        }

        /*
         * Создаем новое количество разделов.
         */
        List<Task> newMemoryBlocks =
                new ArrayList<>(newCount);

        for (int i = 0; i < newCount; i++) {
            newMemoryBlocks.add(null);
        }

        /*
         * Оставляем столько задач,
         * сколько помещается в новые разделы.
         */
        int tasksToKeep =
                Math.min(
                        currentTasks.size(),
                        newCount
                );

        for (int i = 0; i < tasksToKeep; i++) {

            newMemoryBlocks.set(
                    i,
                    currentTasks.get(i)
            );
        }

        /*
         * Остальные задачи возвращаем
         * в очередь ожидания.
         */
        for (
                int i = tasksToKeep;
                i < currentTasks.size();
                i++
        ) {

            Task task =
                    currentTasks.get(i);

            runningTasks.remove(task);
            ioWaitTasks.remove(task);

            waitQueue.add(0, task);
        }

        memoryBlocks = newMemoryBlocks;

        updateCpuStateAfterMemoryChange();

        output(
                "Изменено количество разделов памяти: "
                        + oldCount
                        + " -> "
                        + newCount
        );

        if (currentTasks.size() > newCount) {

            output(
                    "Возвращено в очередь: "
                            + (
                            currentTasks.size()
                                    - newCount
                    )
                            + " задач"
            );
        }
    }

    /**
     * Обновляет состояние CPU после изменения памяти.
     */
    private void updateCpuStateAfterMemoryChange() {

        int usedBlocks =
                countUsedMemoryBlocks();

        if (usedBlocks > maxBlocksCount) {

            changeCpuState(
                    CpuState.OVERLOADED,
                    "(перегрузка после изменения памяти)"
            );

        } else if (
                cpu.getState() == CpuState.OVERLOADED
                        && usedBlocks <= maxBlocksCount
        ) {

            changeToNormalState();
        }
    }

    /**
     * Автоматически уменьшает количество разделов,
     * если нагрузка стала маленькой.
     */
    private boolean checkAndAdjustMemoryBlocks() {

        int currentLoad =
                runningTasks.size();

        if (
                currentLoad < maxBlocksCount / 2
                        && maxBlocksCount > 2
        ) {

            int newCount =
                    Math.max(
                            maxBlocksCount - 1,
                            2
                    );

            changeMemoryBlocksCount(
                    newCount
            );

            return true;
        }

        return false;
    }

    /**
     * Количество занятых разделов памяти.
     */
    private int countUsedMemoryBlocks() {

        int count = 0;

        for (Task task : memoryBlocks) {

            if (task != null) {
                count++;
            }
        }

        return count;
    }

    // =========================================================
    // CALLBACK
    // =========================================================

    /**
     * Устанавливает callback для вывода сообщений.
     */
    public void setOutputCallback(
            Consumer<String> callback
    ) {

        this.outputCallback = callback;
    }

    /**
     * Вывод сообщения.
     */
    public void output(String message) {

        if (outputCallback != null) {
            outputCallback.accept(message);
        }
    }

    // =========================================================
    // СОСТОЯНИЯ CPU
    // =========================================================

    /**
     * Изменяет состояние CPU.
     */
    public void changeCpuState(
            CpuState newState,
            String reason
    ) {

        CpuState oldState =
                cpu.getState();

        if (oldState != newState) {

            cpu.setState(newState);

            output(
                    "ПЕРЕКЛЮЧЕНИЕ CPU: "
                            + oldState.getDisplayName()
                            + " -> "
                            + newState.getDisplayName()
                            + " "
                            + reason
            );

            int currentUsedBlocks =
                    countUsedMemoryBlocks();

            int mathCount = 0;
            int ioCount = 0;

            for (Task task : runningTasks) {

                if (
                        task.getType()
                                == TaskType.CPU_BOUND
                                && task.getState()
                                == TaskState.RUNNING
                ) {

                    mathCount++;

                } else if (
                        task.getType()
                                == TaskType.IO_BOUND
                                && task.getState()
                                == TaskState.RUNNING
                ) {

                    ioCount++;
                }
            }

            output(
                    "Отладочная информация: "
                            + "Используется разделов="
                            + currentUsedBlocks
                            + "/"
                            + maxBlocksCount
                            + ", MATH="
                            + mathCount
                            + ", INOUT="
                            + ioCount
            );
        }
    }

    /**
     * Возвращает CPU в нормальное состояние.
     */
    private void changeToNormalState() {

        boolean hasMathTasks = false;
        boolean hasIoTasks = false;

        for (Task task : runningTasks) {

            if (task.getState() != TaskState.RUNNING) {
                continue;
            }

            if (
                    task.getType()
                            == TaskType.CPU_BOUND
            ) {

                hasMathTasks = true;

            } else if (
                    task.getType()
                            == TaskType.IO_BOUND
            ) {

                hasIoTasks = true;
            }
        }

        if (hasMathTasks) {

            changeCpuState(
                    CpuState.EXECUTING,
                    "(система восстановилась, есть MATH задачи)"
            );

        } else if (hasIoTasks) {

            changeCpuState(
                    CpuState.IO_WAIT,
                    "(система восстановилась, есть INOUT задачи)"
            );

        } else {

            changeCpuState(
                    CpuState.IDLE,
                    "(система восстановилась, нет активных задач)"
            );
        }
    }

    /**
     * Обработка состояния IDLE.
     */
    private void handleIdleState() {

        checkOverload();

        if (cpu.getState() == CpuState.OVERLOADED) {
            return;
        }

        int mathCount =
                countRunningTasks(TaskType.CPU_BOUND);

        int ioCount =
                countRunningTasks(TaskType.IO_BOUND);

        if (mathCount > 0) {

            changeCpuState(
                    CpuState.EXECUTING,
                    "(найдены "
                            + mathCount
                            + " активных MATH задач)"
            );

        } else if (ioCount > 0) {

            changeCpuState(
                    CpuState.IO_WAIT,
                    "(найдены "
                            + ioCount
                            + " активных INOUT задач)"
            );
        }
    }

    /**
     * Обработка состояния EXECUTING.
     */
    private void handleExecutingState() {

        checkOverload();

        if (cpu.getState() == CpuState.OVERLOADED) {
            return;
        }

        int mathCount =
                countRunningTasks(TaskType.CPU_BOUND);

        int ioCount =
                countRunningTasks(TaskType.IO_BOUND);

        if (
                mathCount == 0
                        && ioCount > 0
        ) {

            changeCpuState(
                    CpuState.IO_WAIT,
                    "(MATH задачи завершены, есть активные INOUT)"
            );

        } else if (
                mathCount == 0
                        && ioCount == 0
        ) {

            changeCpuState(
                    CpuState.IDLE,
                    "(все активные задачи завершены)"
            );
        }
    }

    /**
     * Обработка состояния IO_WAIT.
     */
    private void handleIoWaitState() {

        checkOverload();

        if (cpu.getState() == CpuState.OVERLOADED) {
            return;
        }

        int ioCount =
                countRunningTasks(TaskType.IO_BOUND);

        int mathCount =
                countRunningTasks(TaskType.CPU_BOUND);

        if (
                ioCount == 0
                        && mathCount > 0
        ) {

            changeCpuState(
                    CpuState.EXECUTING,
                    "(INOUT задачи завершены, есть активные MATH)"
            );

        } else if (
                ioCount == 0
                        && mathCount == 0
        ) {

            changeCpuState(
                    CpuState.IDLE,
                    "(все активные задачи завершены)"
            );
        }
    }

    /**
     * Количество выполняющихся задач определенного типа.
     */
    private int countRunningTasks(
            TaskType type
    ) {

        int count = 0;

        for (Task task : runningTasks) {

            if (
                    task.getType() == type
                            && task.getState()
                            == TaskState.RUNNING
            ) {

                count++;
            }
        }

        return count;
    }

    // =========================================================
    // ПЕРЕГРУЗКА CPU
    // =========================================================

    /**
     * Управление состоянием CPU.
     */
    private void manageCpuStates() {

        checkOverload();

        if (
                cpu.getState()
                        != CpuState.OVERLOADED
        ) {

            switch (cpu.getState()) {

                case IDLE:
                    handleIdleState();
                    break;

                case EXECUTING:
                    handleExecutingState();
                    break;

                case IO_WAIT:
                    handleIoWaitState();
                    break;

                case OVERLOADED:
                    break;
            }
        }

        String currentState =
                cpu.getState()
                        .getDisplayName();

        if (cpuStateCounts.containsKey(currentState)) {

            cpuStateCounts.put(
                    currentState,
                    cpuStateCounts.get(currentState) + 1
            );
        }
    }

    /**
     * Проверка перегрузки системы.
     */
    private void checkOverload() {

        int currentUsedBlocks =
                countUsedMemoryBlocks();

        if (
                currentUsedBlocks
                        > maxBlocksCount
        ) {

            if (
                    cpu.getState()
                            != CpuState.OVERLOADED
            ) {

                changeCpuState(
                        CpuState.OVERLOADED,
                        "(перегрузка памяти: "
                                + currentUsedBlocks
                                + " > "
                                + maxBlocksCount
                                + " разделов)"
                );
            }

        } else {

            if (
                    cpu.getState()
                            == CpuState.OVERLOADED
            ) {

                changeToNormalState();
            }
        }
    }

    // =========================================================
    // СТАТИСТИКА
    // =========================================================

    /**
     * Собирает статистику за текущий такт.
     */
    @SuppressWarnings("unchecked")
    private void collectStatistics() {

        int usedBlocks =
                countUsedMemoryBlocks();

        ((List<Integer>) history.get(
                "memory_blocks_used"
        )).add(usedBlocks);

        ((List<String>) history.get(
                "cpu_states"
        )).add(
                cpu.getState()
                        .getDisplayName()
        );

        int waitCount =
                waitQueue.size();

        int runCount = 0;

        for (Task task : runningTasks) {

            if (
                    task.getState()
                            == TaskState.RUNNING
            ) {

                runCount++;
            }
        }

        int readyCount =
                readyQueue.size();

        Map<String, List<Integer>> taskStates =
                (Map<String, List<Integer>>)
                        history.get("task_states");

        taskStates.get("WAIT").add(waitCount);
        taskStates.get("RUN").add(runCount);
        taskStates.get("READY").add(readyCount);

        double usedMemoryPercent =
                maxBlocksCount == 0
                        ? 0
                        : (
                        (double) usedBlocks
                                / maxBlocksCount
                                * 100
                );

        double freeMemoryPercent =
                Math.max(
                        0,
                        100 - usedMemoryPercent
                );

        ((List<Double>) history.get(
                "memory_usage"
        )).add(freeMemoryPercent);

        ((List<Integer>) history.get(
                "tacts"
        )).add(currentTact);
    }

    /**
     * Возвращает копию счетчиков состояний CPU.
     */
    public Map<String, Integer> getCpuStateCounts() {

        return new HashMap<>(
                cpuStateCounts
        );
    }

    // =========================================================
    // ОСНОВНОЙ ТАКТ
    // =========================================================

    /**
     * Выполняет один такт моделирования.
     *
     * Аналог Python runTact().
     */
    public void runTact() {

        currentTact++;

        output(
                "\nТакт-"
                        + currentTact
        );

        output(
                "Начальное состояние процессора: "
                        + cpu.getState()
                        .getDisplayName()
        );

        int usedBlocks =
                countUsedMemoryBlocks();

        output(
                "Используется разделов: "
                        + usedBlocks
                        + "/"
                        + maxBlocksCount
        );

        /*
         * Автоматическая настройка памяти.
         */
        boolean memoryAdjusted =
                checkAndAdjustMemoryBlocks();

        if (memoryAdjusted) {

            usedBlocks =
                    countUsedMemoryBlocks();

            output(
                    "После настройки: "
                            + usedBlocks
                            + "/"
                            + maxBlocksCount
                            + " разделов"
            );
        }

        /*
         * Вывод текущего содержимого разделов.
         */
        for (
                int i = 0;
                i < memoryBlocks.size();
                i++
        ) {

            Task task =
                    memoryBlocks.get(i);

            if (task != null) {

                output(
                        "Раздел "
                                + (i + 1)
                                + ": Задача "
                                + getTaskTypeName(task)
                                + " "
                                + task.getMemoryRequired()
                                + "MB "
                                + getTaskStateName(task)
                );
            }
        }

        /*
         * Освобождаем завершенные задачи.
         */
        boolean memoryFreed =
                freeCompletedTasks();

        /*
         * Загружаем новые задачи.
         */
        boolean memoryLoaded =
                loadTasksToMemory();

        if (
                memoryFreed
                        || memoryLoaded
        ) {

            usedBlocks =
                    countUsedMemoryBlocks();

            output(
                    "Разделов памяти после загрузки/выгрузки: "
                            + usedBlocks
                            + "/"
                            + maxBlocksCount
            );
        }

        /*
         * Выполняем задачи.
         */
        executeTasks();

        /*
         * Обновляем состояние CPU.
         */
        manageCpuStates();

        /*
         * Сохраняем статистику.
         */
        collectStatistics();

        output(
                "Финальное состояние процессора: "
                        + cpu.getState()
                        .getDisplayName()
        );
    }

    // =========================================================
    // РАБОТА С ЗАДАЧАМИ
    // =========================================================

    /**
     * Освобождает разделы от завершенных задач.
     */
    private boolean freeCompletedTasks() {

        boolean freed = false;

        for (
                int i = 0;
                i < memoryBlocks.size();
                i++
        ) {

            Task task =
                    memoryBlocks.get(i);

            if (
                    task != null
                            && task.getState()
                            == TaskState.READY
            ) {

                output(
                        "Задача "
                                + task.getId()
                                + " завершена, "
                                + "освобождается раздел "
                                + (i + 1)
                );

                memoryBlocks.set(
                        i,
                        null
                );

                runningTasks.remove(task);

                ioWaitTasks.remove(task);

                readyQueue.add(task);

                freed = true;
            }
        }

        return freed;
    }

    /**
     * Загружает задачи в свободные разделы.
     */
    private boolean loadTasksToMemory() {

        boolean loaded = false;

        for (
                int i = 0;
                i < memoryBlocks.size();
                i++
        ) {

            if (
                    memoryBlocks.get(i) == null
                            && !waitQueue.isEmpty()
            ) {

                Task task =
                        waitQueue.remove(0);

                memoryBlocks.set(
                        i,
                        task
                );

                output(
                        "Задача "
                                + task.getId()
                                + " ("
                                + getTaskTypeName(task)
                                + ") загружена в раздел "
                                + (i + 1)
                );

                loaded = true;

                int currentUsedBlocks =
                        countUsedMemoryBlocks();

                if (
                        currentUsedBlocks
                                > maxBlocksCount
                ) {

                    output(
                            "ПРЕДУПРЕЖДЕНИЕ: "
                                    + "Превышено максимальное "
                                    + "количество разделов! ("
                                    + currentUsedBlocks
                                    + " > "
                                    + maxBlocksCount
                                    + " разделов)"
                    );
                }
            }
        }

        return loaded;
    }

    /**
     * Выполняет задачи, находящиеся в памяти.
     */
    private void executeTasks() {

        for (
                int i = 0;
                i < memoryBlocks.size();
                i++
        ) {

            Task task =
                    memoryBlocks.get(i);

            if (task == null) {
                continue;
            }

            /*
             * Новая задача начинает выполняться.
             */
            if (
                    task.getState()
                            == TaskState.WAITING
            ) {

                cpu.useToDoTask(task);

                output(
                        "Начато выполнение задачи "
                                + task.getId()
                                + " ("
                                + getTaskTypeName(task)
                                + ") в разделе "
                                + (i + 1)
                );

                if (
                        task.getType()
                                == TaskType.IO_BOUND
                ) {

                    if (!ioWaitTasks.contains(task)) {
                        ioWaitTasks.add(task);
                    }
                }

                if (!runningTasks.contains(task)) {
                    runningTasks.add(task);
                }

                /*
                 * Задача уже выполняется.
                 */
            } else if (
                    task.getState()
                            == TaskState.RUNNING
            ) {

                boolean completed =
                        task.execute();

                output(
                        "Задача "
                                + task.getId()
                                + " ("
                                + getTaskTypeName(task)
                                + ") выполняется: "
                                + task.getExecutionTime()
                                + "/"
                                + task.getRequiredTime()
                                + " тактов"
                );

                if (completed) {

                    output(
                            "Задача "
                                    + task.getId()
                                    + " ("
                                    + getTaskTypeName(task)
                                    + ") завершена!"
                    );

                    ioWaitTasks.remove(task);

                    /*
                     * В Python задача здесь
                     * переходит в READY.
                     *
                     * Освобождение памяти произойдет
                     * в следующем такте через
                     * freeCompletedTasks().
                     */
                }
            }
        }
    }

    // =========================================================
    // ЗАВЕРШЕНИЕ СИМУЛЯЦИИ
    // =========================================================

    /**
     * Проверяет, закончилась ли симуляция.
     *
     * Аналог Python isSimOver().
     */
    public boolean isSimulationFinished() {

        if (!waitQueue.isEmpty()) {
            return false;
        }

        if (!runningTasks.isEmpty()) {
            return false;
        }

        if (!ioWaitTasks.isEmpty()) {
            return false;
        }

        for (Task task : memoryBlocks) {

            if (
                    task != null
                            && task.getState()
                            != TaskState.READY
            ) {

                return false;
            }
        }

        return true;
    }

    /**
     * Сбрасывает ОС в начальное состояние.
     */
    public void reset() {

        waitQueue =
                new ArrayList<>(
                        packet.getTasks()
                );

        readyQueue =
                new ArrayList<>();

        runningTasks =
                new ArrayList<>();

        ioWaitTasks =
                new ArrayList<>();

        memoryBlocks =
                new ArrayList<>(
                        maxBlocksCount
                );

        for (int i = 0; i < maxBlocksCount; i++) {
            memoryBlocks.add(null);
        }

        cpu.setState(
                CpuState.IDLE
        );

        cpu.setCurrentTask(null);

        currentTact = 0;

        initializeHistory();
        initializeCpuStateCounts();

        /*
         * Возвращаем все задачи
         * в начальное состояние.
         */
        for (Task task : packet.getTasks()) {

            task.setState(
                    TaskState.WAITING
            );

            task.setExecutionTime(0);
        }

        output(
                "Система сброшена в начальное состояние"
        );
    }

    // =========================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================

    private String getTaskTypeName(
            Task task
    ) {

        if (
                task.getType()
                        == TaskType.CPU_BOUND
        ) {

            return "MATH";
        }

        return "INOUT";
    }

    private String getTaskStateName(
            Task task
    ) {

        return switch (task.getState()) {

            case WAITING ->
                    "ОЖИДАНИЕ ВЫПОЛНЕНИЯ";

            case RUNNING ->
                    "В ПРОЦЕССЕ ВЫПОЛНЕНИЯ";

            case READY ->
                    "ВЫПОЛНЕНА";

            case NEW ->
                    "НОВАЯ";

            case TERMINATED ->
                    "ЗАВЕРШЕНА";
        };
    }
}