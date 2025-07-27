package io.cldf.tool.utils;

import java.util.Scanner;

public class ConsoleUtils {

  public static void printHeader(String text) {
    System.out.println("\n" + "=".repeat(text.length() + 4));
    System.out.println("| " + text + " |");
    System.out.println("=".repeat(text.length() + 4) + "\n");
  }

  public static void printSection(String text) {
    System.out.println("\n--- " + text + " ---");
  }

  public static void printSuccess(String message) {
    System.out.println("✓ " + message);
  }

  public static void printError(String message) {
    System.err.println("✗ " + message);
  }

  public static void printWarning(String message) {
    System.out.println("⚠ " + message);
  }

  public static void printInfo(String message) {
    System.out.println("ℹ " + message);
  }

  public static boolean confirm(Scanner scanner, String message) {
    System.out.print(message + " (y/n): ");
    String response = scanner.nextLine().trim().toLowerCase();
    return response.startsWith("y");
  }

  public static String prompt(Scanner scanner, String message, String defaultValue) {
    if (defaultValue != null && !defaultValue.isEmpty()) {
      System.out.print(message + " [" + defaultValue + "]: ");
    } else {
      System.out.print(message + ": ");
    }
    String response = scanner.nextLine().trim();
    return response.isEmpty() && defaultValue != null ? defaultValue : response;
  }

  public static int promptInt(Scanner scanner, String message, int defaultValue) {
    String prompt = defaultValue >= 0 ? message + " [" + defaultValue + "]" : message;
    String response = prompt(scanner, prompt, null);
    if (response.isEmpty() && defaultValue >= 0) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(response);
    } catch (NumberFormatException e) {
      printError("Invalid number: " + response);
      return promptInt(scanner, message, defaultValue);
    }
  }

  public static void printProgress(int current, int total) {
    int percent = (int) ((current / (double) total) * 100);
    StringBuilder bar = new StringBuilder("[");
    int barLength = 30;
    int filled = (int) ((current / (double) total) * barLength);

    for (int i = 0; i < barLength; i++) {
      if (i < filled) {
        bar.append("=");
      } else if (i == filled) {
        bar.append(">");
      } else {
        bar.append(" ");
      }
    }
    bar.append("]");

    System.out.print("\r" + bar + " " + percent + "% (" + current + "/" + total + ")");
    if (current == total) {
      System.out.println();
    }
  }
}
