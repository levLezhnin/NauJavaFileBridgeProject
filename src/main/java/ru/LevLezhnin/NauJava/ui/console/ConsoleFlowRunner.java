package ru.LevLezhnin.NauJava.ui.console;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Scanner;
import java.util.stream.Collectors;

@Component
public class ConsoleFlowRunner {

    private final CommandProcessor commandProcessor;

    @Autowired
    public ConsoleFlowRunner(CommandProcessor commandProcessor) {
        this.commandProcessor = commandProcessor;
    }

    public void start() {
        Scanner sc = new Scanner(System.in);
        String options = Arrays.stream(Command.values()).map(Enum::name).collect(Collectors.joining(", "));

        while (true) {
            System.out.printf("Введите команду (%s):\n", options);
            String input = sc.nextLine();
            String[] args = input.split(" ");
            try {
                commandProcessor.processCommand(args);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }
}
