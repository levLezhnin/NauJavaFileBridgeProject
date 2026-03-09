package ru.LevLezhnin.NauJava.ui.console;

import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.exceptions.MultipleExceptions;

@Component
public class CommandValidator {

    public void isValidCommand(Command command, String... args) {
        if (command == Command.EXIT) {
            return;
        }

        if (args.length == 0) {
            throw new IllegalArgumentException("Нет команд без аргументов, кроме EXIT");
        }

        MultipleExceptions multipleExceptions = new MultipleExceptions();

        switch (command) {
            case CREATE -> {
                if (args.length < 4) {
                    multipleExceptions.addException(new IllegalArgumentException("Ожидается 4 аргумента: id, логин, email, пароль"));
                }
            }
            case UPDATE_PASSWORD -> {
                if (args.length < 2) {
                    multipleExceptions.addException(new IllegalArgumentException("Ожидается 2 аргумента: id, пароль"));
                }
            }
            case UPDATE_USERNAME -> {
                if (args.length < 2) {
                    multipleExceptions.addException(new IllegalArgumentException("Ожидается 2 аргумента: id, логин"));
                }
            }
        }

        try {
            Long.parseLong(args[0]);
        } catch (NumberFormatException ignored) {
            multipleExceptions.addException(new IllegalArgumentException("id должен быть написан целым числом"));
        }

        if (multipleExceptions.hasExceptions()) {
            throw multipleExceptions.toRuntimeException();
        }
    }

}
