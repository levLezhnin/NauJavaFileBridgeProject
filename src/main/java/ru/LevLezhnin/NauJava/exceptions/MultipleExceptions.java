package ru.LevLezhnin.NauJava.exceptions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MultipleExceptions {
    private final List<Throwable> throwables;

    public MultipleExceptions() {
        this.throwables = new ArrayList<>();
    }

    public void addException(Throwable throwable) {
        throwables.add(throwable);
    }

    public boolean hasExceptions() {
        return !throwables.isEmpty();
    }

    public RuntimeException toRuntimeException() {
        String messages = throwables.stream().map(Throwable::toString).collect(Collectors.joining("\n"));
        return new RuntimeException(messages);
    }
}
