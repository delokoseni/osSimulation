package io.github.delokoseni.model;

public enum CpuState {

    IDLE("ПРОСТОЙ"),

    EXECUTING("ВЫПОЛНЕНИЕ ВЫЧИСЛЕНИЙ"),

    IO_WAIT("ОЖИДАНИЕ ЗАВЕРШЕНИЯ ВВОДА/ВЫВОДА"),

    OVERLOADED("ПЕРЕГРУЗКА");

    private final String displayName;

    CpuState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}