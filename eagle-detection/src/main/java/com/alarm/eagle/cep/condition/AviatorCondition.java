package com.alarm.eagle.cep.condition;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic condition based on the Aviator expression engine (performance-optimized edition).
 *
 * <p>Conditions are stored as strings (e.g. {@code "action == 0"}) and compiled/executed dynamically by Aviator at runtime.
 *
 * <p><b>Phase 4 performance optimizations</b>:
 * <ul>
 *   <li><b>MethodHandle cache</b>: replaces reflection {@code Method.invoke()} with {@link MethodHandle},
 *     globally cached by {@code ClassName#fieldName}. After the first lookup, subsequent calls execute MethodHandle directly,
 *     avoiding reflection lookup overhead (~10-30% improvement)</li>
 *   <li><b>Variable pre-validation</b>: if an event Class is provided at construction time, automatically checks
 *     whether variable names in the Aviator expression exist in the event class (getter or field),
 *     catching configuration errors early</li>
 * </ul>
 *
 * <p><b>Serialization compatibility</b>: {@code compiledExpression} and cache fields are {@code transient},
 * recompiled on deserialization but caches rebuilt automatically.
 *
 * @param <T> event type
 */
public class AviatorCondition<T> extends SimpleCondition<T> {

    private static final long serialVersionUID = 2L;

    // ======================== ★ Global MethodHandle Cache ========================

    /**
     * ★ Two-level cache: ClassName → (fieldName → MethodHandle).
     * Shared by all AviatorCondition instances to avoid repeated lookups.
     * MethodHandle is ~10-30% faster than traditional reflection invoke(),
     * and avoids Method.getMethod() / Field.getDeclaredField() lookup on every filter() call.
     */
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, MethodHandle>>
            GETTER_CACHE = new ConcurrentHashMap<>();

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    // ======================== Instance Fields ========================

    /** Expression string, e.g. "action == 0 && name == 'Ken'" */
    private final String expression;

    /** ★ transient: Aviator compiled output is not serializable; recompile on deserialization */
    private transient Expression compiledExpression;

    /**
     * Constructor (backward compatible).
     *
     * @param expression Aviator expression, e.g. "action == 0"
     */
    public AviatorCondition(String expression) {
        this(expression, null);
    }

    /**
     * ★ Enhanced constructor: optionally pass event Class for variable pre-validation.
     *
     * @param expression Aviator expression
     * @param eventClass event class (nullable; null skips pre-validation)
     */
    public AviatorCondition(String expression, Class<T> eventClass) {
        this.expression = Objects.requireNonNull(expression, "expression must not be null");
        validateAndCompile();

        // ★ Pre-validation: check if variable names in expression exist in event class
        if (eventClass != null) {
            preValidateVariables(eventClass);
        }
    }

    public String getExpression() {
        return expression;
    }

    // ======================== Core Filter Method ========================

    /**
     * ★ Core filter method. Called for every incoming event.
     */
    @Override
    public boolean filter(T eventBean) throws Exception {
        List<String> variableNames = compiledExpression.getVariableNames();
        if (variableNames.isEmpty()) {
            return true;
        }

        Map<String, Object> variables = new HashMap<>(variableNames.size());
        for (String varName : variableNames) {
            Object value = getVariableValue(eventBean, varName);
            if (value != null) {
                variables.put(varName, value);
            }
        }
        return (Boolean) compiledExpression.execute(variables);
    }

    // ======================== ★ MethodHandle Variable Access (replaces reflection) ========================

    /**
     * ★ Get field value via MethodHandle (with caching).
     *
     * <p>Lookup priority: getter → is-getter (boolean) → direct field access.
     * Results are cached in the global MethodHandle cache after the first lookup.
     */
    public Object getVariableValue(T bean, String fieldName)
            throws ReflectiveOperationException {
        String className = bean.getClass().getName();

        // ① Read from cache
        ConcurrentHashMap<String, MethodHandle> classCache =
                GETTER_CACHE.get(className);
        if (classCache != null) {
            MethodHandle mh = classCache.get(fieldName);
            if (mh != null) {
                return invokeHandle(mh, bean);  // ★ MethodHandle invocation
            }
        }

        // ② Cache miss → build MethodHandle
        MethodHandle mh = buildMethodHandle(bean.getClass(), fieldName);
        if (mh == null) {
            throw new NoSuchFieldException("No getter or field '"
                    + fieldName + "' in class " + className);
        }

        // ③ Store in cache
        GETTER_CACHE.computeIfAbsent(className,
                k -> new ConcurrentHashMap<>()).put(fieldName, mh);

        return invokeHandle(mh, bean);
    }

    /** ★ Safe MethodHandle invocation, wrapping Throwable as ReflectiveOperationException */
    private static Object invokeHandle(MethodHandle mh, Object bean)
            throws ReflectiveOperationException {
        try {
            return mh.invoke(bean);
        } catch (RuntimeException | ReflectiveOperationException e) {
            throw e;
        } catch (Throwable t) {
            throw new ReflectiveOperationException(t);
        }
    }

    /**
     * ★ Build a MethodHandle for the given class and field name.
     */
    private static MethodHandle buildMethodHandle(
            Class<?> clazz, String fieldName) {
        try {
            // 1) Try getter: getXxx()
            String getter = "get" + Character.toUpperCase(fieldName.charAt(0))
                    + fieldName.substring(1);
            return LOOKUP.unreflect(clazz.getMethod(getter));
        } catch (NoSuchMethodException | IllegalAccessException e1) {
            // fall through to "is" prefix
        }

        try {
            // 2) Try is-getter: isXxx() (for boolean fields)
            if (!fieldName.startsWith("is")) {
                String isGetter = "is" + Character.toUpperCase(fieldName.charAt(0))
                        + fieldName.substring(1);
                try {
                    return LOOKUP.unreflect(clazz.getMethod(isGetter));
                } catch (NoSuchMethodException | IllegalAccessException e2) {
                    // fall through
                }
            }
        } catch (Exception ignored) {
            // fall through to direct field
        }

        try {
            // 3) Direct field access
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return LOOKUP.unreflectGetter(field);
        } catch (NoSuchFieldException | IllegalAccessException e3) {
            return null;
        }
    }

    // ======================== ★ Variable Pre-Validation ========================

    /**
     * ★ Pre-validation: check that all variable names in the expression exist in the event class.
     */
    private void preValidateVariables(Class<T> eventClass) {
        List<String> variableNames = compiledExpression.getVariableNames();
        for (String varName : variableNames) {
            if (!hasFieldOrGetter(eventClass, varName)) {
                throw new IllegalArgumentException(
                        "Aviator variable '" + varName + "' not found in event class "
                                + eventClass.getName()
                                + ". Available fields: check getters or public fields.");
            }
        }
    }

    /**
     * Check whether the specified field exists in the event class (getter or direct field).
     */
    private static boolean hasFieldOrGetter(Class<?> clazz, String fieldName) {
        // Check getter
        String getter = "get" + Character.toUpperCase(fieldName.charAt(0))
                + fieldName.substring(1);
        try {
            clazz.getMethod(getter);
            return true;
        } catch (NoSuchMethodException ignored) {
            // fall through
        }

        // Check is-getter
        if (!fieldName.startsWith("is")) {
            String isGetter = "is" + Character.toUpperCase(fieldName.charAt(0))
                    + fieldName.substring(1);
            try {
                clazz.getMethod(isGetter);
                return true;
            } catch (NoSuchMethodException ignored) {
                // fall through
            }
        }

        // Check direct field
        try {
            clazz.getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException ignored) {
            // fall through
        }

        return false;
    }

    // ======================== Serialization Compatibility ========================

    /**
     * ★ Recompile expression on deserialization (since compiledExpression is transient).
     */
    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        validateAndCompile();
    }

    // ======================== Internal Methods ========================

    private void validateAndCompile() {
        try {
            AviatorEvaluator.validate(expression);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid Aviator expression: " + expression, e);
        }
        this.compiledExpression = AviatorEvaluator.compile(expression, false);
    }

    // ======================== Statistics Interface ========================

    /** @return current global cache size (for debugging) */
    public static int getCacheSize() {
        int total = 0;
        for (ConcurrentHashMap<String, MethodHandle> classCache :
                GETTER_CACHE.values()) {
            total += classCache.size();
        }
        return total;
    }

    /** Clear the global MethodHandle cache (for testing) */
    public static void clearCache() {
        GETTER_CACHE.clear();
    }
}
