package io.github.delokoseni.simulation;

import io.github.delokoseni.model.TaskPackage;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Simulation {

    /**
     * Операционная система.
     */
    private final OperatingSystem operatingSystem;

    /**
     * Начальное количество разделов памяти.
     */
    private int maxBlocksCount;

    /**
     * Объем оперативной памяти.
     */
    private final int ram;

    /**
     * Максимальное количество тактов.
     */
    private final int maxTacts;

    /**
     * Время начала симуляции.
     */
    private long startTime;

    /**
     * Время окончания симуляции.
     */
    private long endTime;

    /**
     * Фактическое количество выполненных тактов.
     */
    private int totalTacts;

    /**
     * История изменения количества разделов памяти.
     */
    private final List<String> memoryChanges;

    public Simulation(
            TaskPackage taskPackage,
            int maxBlocksCount,
            int ram,
            int maxTacts
    ) {

        this.maxBlocksCount = maxBlocksCount;
        this.ram = ram;
        this.maxTacts = maxTacts;

        /*
         * Аналог Python:
         *
         * self.os = OS(
         *     ram=self.ram,
         *     max_blocks_count=self.max_blocks_count
         * )
         */
        this.operatingSystem =
                new OperatingSystem(
                        taskPackage,
                        ram,
                        maxBlocksCount
                );

        this.totalTacts = 0;

        this.memoryChanges = new ArrayList<>();

        this.memoryChanges.add(
                "Начальное количество: "
                        + maxBlocksCount
                        + " разделов"
        );
    }

    /**
     * Запуск симуляции.
     */
    public void start() {

        totalTacts = 0;

        startTime = System.nanoTime();

        output("СТАРТ");

        /*
         * Информация о пакете.
         */
        int totalMemory =
                operatingSystem
                        .getPacket()
                        .getTasksMemory();

        double totalMemoryGb =
                totalMemory / 1024.0;

        output(
                String.format(
                        "Суммарно RAM пакета: %.1f ГБ",
                        totalMemoryGb
                )
        );

        output(
                "Всего задач: "
                        + operatingSystem
                        .getPacket()
                        .getTaskCount()
        );

        output(
                "MATH задач: "
                        + operatingSystem
                        .getPacket()
                        .getMathTasks()
        );

        output(
                "INOUT задач: "
                        + operatingSystem
                        .getPacket()
                        .getInOutTasks()
        );

        output(
                "Начальное количество разделов памяти: "
                        + maxBlocksCount
        );

        /*
         * Аналог Python:
         *
         * for tact in range(self.max_tacts):
         */
        for (int tact = 0; tact < maxTacts; tact++) {

            totalTacts = tact + 1;

            operatingSystem.runTact();

            if (isSimulationOver()) {
                break;
            }
        }

        endTime = System.nanoTime();

        output("\nФИНИШ");

        output(
                "Все задачи выполнены за "
                        + totalTacts
                        + " тактов"
        );

        output(
                "Финальное количество разделов памяти: "
                        + maxBlocksCount
        );

        /*
         * История изменения разделов.
         */
        if (memoryChanges.size() > 1) {

            output(
                    "\nИстория изменений разделов памяти:"
            );

            for (String change : memoryChanges) {
                output("  " + change);
            }
        }
    }

    /**
     * Изменяет количество разделов памяти.
     */
    public void changeMemoryBlocks(int newCount) {

        int oldCount = maxBlocksCount;

        operatingSystem.changeMemoryBlocksCount(
                newCount
        );

        maxBlocksCount = newCount;

        String changeInfo =
                "Такт "
                        + totalTacts
                        + ": "
                        + oldCount
                        + " → "
                        + newCount
                        + " разделов";

        memoryChanges.add(changeInfo);
    }

    /**
     * Проверяет окончание симуляции.
     */
    public boolean isSimulationOver() {

        return operatingSystem.isSimulationFinished();
    }

    /**
     * Возвращает время работы симуляции
     * в секундах.
     */
    public double getRunTime() {

        if (endTime == 0) {
            return 0.0;
        }

        return (endTime - startTime)
                / 1_000_000_000.0;
    }

    /**
     * Сбрасывает симуляцию.
     */
    public void reset() {

        operatingSystem.reset();

        startTime = System.nanoTime();
        endTime = 0;

        totalTacts = 0;

        memoryChanges.clear();

        memoryChanges.add(
                "Начальное количество: "
                        + maxBlocksCount
                        + " разделов"
        );
    }

    /**
     * Вывод сообщения через callback.
     */
    private void output(String message) {

        if (operatingSystem.getOutputCallback() != null) {

            operatingSystem
                    .getOutputCallback()
                    .accept(message);
        }
    }
}