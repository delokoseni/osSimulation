package io.github.delokoseni.simulation;

import io.github.delokoseni.model.TaskPackage;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Simulation {

    private final OperatingSystem operatingSystem;

    private int maxBlocksCount;

    private final int ram;

    private final int maxTacts;

    private long startTime;

    private long endTime;

    private int totalTacts;

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

    public void start() {
        totalTacts = 0;

        startTime = System.nanoTime();

        output("СТАРТ");

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

        if (memoryChanges.size() > 1) {
            output(
                    "\nИстория изменений разделов памяти:"
            );

            for (String change : memoryChanges) {
                output("  " + change);
            }
        }
    }

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

    public boolean isSimulationOver() {
        return operatingSystem.isSimulationFinished();
    }

    public double getRunTime() {
        if (endTime == 0) {
            return 0.0;
        }

        return (endTime - startTime)
                / 1_000_000_000.0;
    }

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

    private void output(String message) {
        if (operatingSystem.getOutputCallback() != null) {
            operatingSystem
                    .getOutputCallback()
                    .accept(message);
        }
    }
}