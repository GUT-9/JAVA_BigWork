package org.example.util;

import java.util.Scanner;

public class ConsoleUtil {
    private static final Scanner SC = new Scanner(System.in);

    public static String readLine(String tip) {
        System.out.print(tip);
        return SC.nextLine();
    }

    public static void printLine(String line) {
        System.out.println(line);
    }
}