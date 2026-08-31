
package org.latios.arenaBrawl.general;

public class MessageUtils {

    private static final String ARROW = "⇛";

    public static String positive() {
        return "§a" + ARROW + " §f";
    }

    public static String negative() {
        return "§c" + ARROW + " §f";
    }

    public static String noValidPlayer() {
        return "§eThere is not valid player within range!";
    }
}