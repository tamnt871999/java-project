package com.example.ordering.adapter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bo doc va ghi JSON toi gian, viet tay de project khong phu thuoc thu vien ngoai.
 *
 * parse() : chuoi JSON -> Map / List / String / BigDecimal / Boolean / null
 * write() : Map / List / String / Number / Boolean / null -> chuoi JSON
 *
 * Trong du an that ban se dung Jackson hoac Gson, va no cung chi duoc phep
 * xuat hien o tang adapter nay ma thoi.
 */
final class Json {

    /** Ky tu mo dau mot escape sequence trong JSON. */
    private static final char ESCAPE = '\\';

    private final String input;
    private int pos;

    private Json(String input) {
        this.input = input;
    }

    static Object parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Request body rong");
        }
        Json parser = new Json(text);
        parser.skipWhitespace();
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (parser.pos < text.length()) {
            throw new IllegalArgumentException("Du thua ky tu tai vi tri " + parser.pos);
        }
        return value;
    }

    private Object readValue() {
        skipWhitespace();
        return switch (peek()) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> readLiteral("true", Boolean.TRUE);
            case 'f' -> readLiteral("false", Boolean.FALSE);
            case 'n' -> readLiteral("null", null);
            default -> readNumber();
        };
    }

    private Map<String, Object> readObject() {
        Map<String, Object> result = new LinkedHashMap<>();
        expect('{');
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return result;
        }
        while (true) {
            skipWhitespace();
            String key = readString();
            skipWhitespace();
            expect(':');
            result.put(key, readValue());
            skipWhitespace();
            char c = next();
            if (c == '}') {
                return result;
            }
            if (c != ',') {
                throw new IllegalArgumentException("Mong doi , hoac } tai vi tri " + pos);
            }
        }
    }

    private List<Object> readArray() {
        List<Object> result = new ArrayList<>();
        expect('[');
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return result;
        }
        while (true) {
            result.add(readValue());
            skipWhitespace();
            char c = next();
            if (c == ']') {
                return result;
            }
            if (c != ',') {
                throw new IllegalArgumentException("Mong doi , hoac ] tai vi tri " + pos);
            }
        }
    }

    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = next();
            if (c == '"') {
                return sb.toString();
            }
            if (c == ESCAPE) {
                char escaped = next();
                switch (escaped) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'u' -> {
                        sb.append((char) Integer.parseInt(input.substring(pos, pos + 4), 16));
                        pos += 4;
                    }
                    default -> sb.append(escaped);
                }
            } else {
                sb.append(c);
            }
        }
    }

    private BigDecimal readNumber() {
        int start = pos;
        while (pos < input.length() && "+-.eE0123456789".indexOf(input.charAt(pos)) >= 0) {
            pos++;
        }
        if (start == pos) {
            throw new IllegalArgumentException("Gia tri khong hop le tai vi tri " + pos);
        }
        return new BigDecimal(input.substring(start, pos));
    }

    private Object readLiteral(String literal, Object value) {
        if (!input.startsWith(literal, pos)) {
            throw new IllegalArgumentException("Tu khoa khong hop le tai vi tri " + pos);
        }
        pos += literal.length();
        return value;
    }

    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }

    private char peek() {
        if (pos >= input.length()) {
            throw new IllegalArgumentException("JSON ket thuc dot ngot");
        }
        return input.charAt(pos);
    }

    private char next() {
        char c = peek();
        pos++;
        return c;
    }

    private void expect(char expected) {
        char actual = next();
        if (actual != expected) {
            throw new IllegalArgumentException(
                    "Mong doi %c nhung gap %c tai vi tri %d".formatted(expected, actual, pos - 1));
        }
    }

    /** Chuyen Map / List / String / Number / Boolean / null thanh chuoi JSON. */
    static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(value, sb);
        return sb.toString();
    }

    private static void writeValue(Object value, StringBuilder sb) {
        switch (value) {
            case null -> sb.append("null");
            case String s -> writeString(s, sb);
            case Number n -> sb.append(n);
            case Boolean b -> sb.append(b);
            case Map<?, ?> map -> {
                sb.append('{');
                boolean first = true;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!first) {
                        sb.append(',');
                    }
                    first = false;
                    writeString(String.valueOf(entry.getKey()), sb);
                    sb.append(':');
                    writeValue(entry.getValue(), sb);
                }
                sb.append('}');
            }
            case List<?> list -> {
                sb.append('[');
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    writeValue(list.get(i), sb);
                }
                sb.append(']');
            }
            default -> writeString(String.valueOf(value), sb);
        }
    }

    private static void writeString(String value, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case ESCAPE -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        sb.append('"');
    }
}
