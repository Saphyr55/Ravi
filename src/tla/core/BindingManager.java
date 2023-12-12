package tla.core;

import tla.syntax.Kind;
import tla.syntax.Syntax;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class BindingManager {

    public static final Map<String, Kind> KEYWORDS = new HashMap<>();
    static {
        try {
            for (Field field : Syntax.Word.class.getDeclaredFields()) {
                KEYWORDS.put((String) field.get(null), field.getAnnotation(Bind.class).kind());
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
