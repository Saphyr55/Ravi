package ravi.core;

import ravi.analysis.Kind;
import ravi.analysis.Token;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public final class BindingManager {

    public static final Map<String, Kind> KEYWORDS = new HashMap<>();
    static {
        try {
            for (Field field : Token.Word.class.getDeclaredFields()) {
                KEYWORDS.put((String) field.get(null), field.getAnnotation(Bind.class).kind());
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
