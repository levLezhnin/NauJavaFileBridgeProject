package ru.LevLezhnin.NauJava.ui.console;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.exceptions.EntityNotFoundException;
import ru.LevLezhnin.NauJava.service.interfaces.UserService;

import java.util.Arrays;

@Component
public class CommandProcessor {

    private final UserService userService;
    private final CommandValidator commandValidator;

    @Autowired
    public CommandProcessor(UserService userService, CommandValidator commandValidator) {
        this.userService = userService;
        this.commandValidator = commandValidator;
    }

    public void processCommand(String... args) {
        Command command;
        try {
            command = Command.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Нет команды с названием: " + args[0].toUpperCase());
        }
        String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);

        commandValidator.isValidCommand(command, commandArgs);

        if (command == Command.EXIT) {
            System.exit(0);
        }

        long id = Long.parseLong(args[1]);
        switch (command) {
            case CREATE -> {
                userService.createUser(args[2], args[3], args[4]);
                System.out.println("Пользователь успешно добавлен");
            }
            case UPDATE_USERNAME -> {
                userService.updateUsername(id, args[2]);
                System.out.printf("Логин пользователя с id: %d успешно обновлён на: %s\n".formatted(id, args[2]));
            }
            case UPDATE_PASSWORD -> {
                userService.updatePassword(id, args[2]);
                System.out.printf("Пароль пользователя с id: %d успешно обновлён\n".formatted(id));
            }
            case FIND -> {
                try {
                    System.out.println(userService.findById(id));
                } catch (EntityNotFoundException e) {
                    System.err.println(e.getMessage());
                }
            }
            case DELETE -> {
                userService.deleteById(id);
                System.out.printf("Пользователь с id: %d успешно удалён\n".formatted(id));
            }
        }
    }
}
