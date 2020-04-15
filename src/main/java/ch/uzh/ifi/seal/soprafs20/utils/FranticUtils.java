package ch.uzh.ifi.seal.soprafs20.utils;

import ch.uzh.ifi.seal.soprafs20.constant.Color;
import ch.uzh.ifi.seal.soprafs20.constant.Type;
import ch.uzh.ifi.seal.soprafs20.constant.Value;
import ch.uzh.ifi.seal.soprafs20.entity.Card;

import java.util.EnumMap;
import java.util.Random;

/**
 * Utility functions that are used throughout the game
 */
public class FranticUtils {

    public static final Random random = new Random();
    private static final EnumMap<Value, String> valueMap = new EnumMap<>(Value.class);
    private static final EnumMap<Color, String> colorMap = new EnumMap<>(Color.class);
    private static final EnumMap<Type, String> typeMap = new EnumMap<>(Type.class);

    public static String generateId(int length) {
        StringBuilder s = new StringBuilder();
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < length; i++) {
            int idx = random.nextInt(chars.length());
            s.append(chars.charAt(idx));
        }
        return s.toString();
    }

    private static void fillValueMap() {
        valueMap.put(Value.ONE, "1");
        valueMap.put(Value.TWO, "2");
        valueMap.put(Value.THREE, "3");
        valueMap.put(Value.FOUR, "4");
        valueMap.put(Value.FIVE, "5");
        valueMap.put(Value.SIX, "6");
        valueMap.put(Value.SEVEN, "7");
        valueMap.put(Value.EIGHT, "8");
        valueMap.put(Value.NINE, "9");
        valueMap.put(Value.SECONDCHANCE, "2nd-Chance");
        valueMap.put(Value.SKIP, "skip");
        valueMap.put(Value.EXCHANGE, "exchange");
        valueMap.put(Value.FANTASTIC, "fantastic");
        valueMap.put(Value.FANTASTICFOUR, "fantastic-four");
        valueMap.put(Value.EQUALITY, "equality");
        valueMap.put(Value.COUNTERATTACK, "counterattack");
        valueMap.put(Value.NICETRY, "nice-try");
        valueMap.put(Value.FUCKYOU, "fuck-you");
        valueMap.put(Value.GIFT, "gift");
    }

    private static void fillColorMap() {
        colorMap.put(Color.BLUE, "blue");
        colorMap.put(Color.GREEN, "green");
        colorMap.put(Color.YELLOW, "yellow");
        colorMap.put(Color.RED, "red");
        colorMap.put(Color.BLACK, "black");
        colorMap.put(Color.MULTICOLOR, "multicolor");
    }

    private static void fillTypeMap() {
        typeMap.put(Type.BACK, "back");
        typeMap.put(Type.SPECIAL, "special");
        typeMap.put(Type.NUMBER, "number");
    }

    public static String getStringRepresentation(Value v) {
        if (!valueMap.containsKey(v)) {
            fillValueMap();
        }
        return valueMap.get(v);
    }

    public static String getStringRepresentation(Color c) {
        if (!colorMap.containsKey(c)) {
            fillColorMap();
        }
        return colorMap.get(c);
    }

    public static String getStringRepresentation(Type t) {
        if (!typeMap.containsKey(t)) {
            fillTypeMap();
        }
        return typeMap.get(t);
    }

    public static String getStringRepresentationOfNumberCard(Card card) {
        if (!colorMap.containsKey(card.getColor())) {
            fillColorMap();
        }
        if (!valueMap.containsKey(card.getValue())) {
            fillValueMap();
        }
        return colorMap.get(card.getColor()) + " " + valueMap.get(card.getValue());
    }
}
