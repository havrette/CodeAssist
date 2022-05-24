package com.tyron.builder.internal.metaobject;

import groovy.lang.GroovyRuntimeException;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;
import org.codehaus.groovy.runtime.metaclass.MissingMethodExecutionFailed;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * An empty {@link DynamicObject}.
 */
public abstract class AbstractDynamicObject implements DynamicObject {
    public abstract String getDisplayName();

    @Override
    public String toString() {
        return "DynamicObject for " + getDisplayName();
    }

    @Override
    public boolean hasProperty(String name) {
        return false;
    }

    @Override
    public DynamicInvokeResult tryGetProperty(String name) {
        return DynamicInvokeResult.notFound();
    }

    @Nullable
    public Class<?> getPublicType() {
        return null;
    }

    public boolean hasUsefulDisplayName() {
        return true;
    }

    @Override
    public Object getProperty(String name) throws MissingPropertyException {
        DynamicInvokeResult result = tryGetProperty(name);
        if (!result.isFound()) {
            throw getMissingProperty(name);
        }
        return result.getValue();
    }

    @Override
    public DynamicInvokeResult trySetProperty(String name, Object value) {
        return DynamicInvokeResult.notFound();
    }

    @Override
    public void setProperty(String name, Object value) throws MissingPropertyException {
        DynamicInvokeResult result = trySetProperty(name, value);
        if (!result.isFound()) {
            throw setMissingProperty(name);
        }
    }

    @Override
    public MissingPropertyException getMissingProperty(String name) {
        Class<?> publicType = getPublicType();
        boolean includeDisplayName = hasUsefulDisplayName();
        if (publicType != null && includeDisplayName) {
            return new MissingPropertyException(String.format("Could not get unknown property '%s' for %s of type %s.", name,
                    getDisplayName(), publicType.getName()), name, publicType);
        } else if (publicType != null) {
            return new MissingPropertyException(String.format("Could not get unknown property '%s' for object of type %s.", name,
                    publicType.getName()), name, publicType);
        } else {
            // Use the display name anyway
            return new MissingPropertyException(String.format("Could not get unknown property '%s' for %s.", name,
                    getDisplayName()), name, null);
        }
    }

    protected GroovyRuntimeException getWriteOnlyProperty(String name) {
        Class<?> publicType = getPublicType();
        boolean includeDisplayName = hasUsefulDisplayName();
        if (publicType != null && includeDisplayName) {
            return new GroovyRuntimeException(String.format(
                    "Cannot get the value of write-only property '%s' for %s of type %s.", name, getDisplayName(), publicType.getName()));
        } else if (publicType != null) {
            return new GroovyRuntimeException(String.format(
                    "Cannot get the value of write-only property '%s' for object of type %s.", name, publicType.getName()));
        } else {
            // Use the display name anyway
            return new GroovyRuntimeException(String.format(
                    "Cannot get the value of write-only property '%s' for %s.", name, getDisplayName()));
        }
    }

    @Override
    public MissingPropertyException setMissingProperty(String name) {
        Class<?> publicType = getPublicType();
        boolean includeDisplayName = hasUsefulDisplayName();
        if (publicType != null && includeDisplayName) {
            return new MissingPropertyException(String.format("Could not set unknown property '%s' for %s of type %s.", name,
                    getDisplayName(), publicType.getName()), name, publicType);
        } else if (publicType != null) {
            return new MissingPropertyException(String.format("Could not set unknown property '%s' for object of type %s.", name,
                    publicType.getName()), name, publicType);
        } else {
            // Use the display name anyway
            return new MissingPropertyException(String.format("Could not set unknown property '%s' for %s.", name,
                    getDisplayName()), name, null);
        }
    }

    protected GroovyRuntimeException setReadOnlyProperty(String name) {
        Class<?> publicType = getPublicType();
        boolean includeDisplayName = hasUsefulDisplayName();
        if (publicType != null && includeDisplayName) {
            return new GroovyRuntimeException(String.format(
                    "Cannot set the value of read-only property '%s' for %s of type %s.", name, getDisplayName(), publicType.getName()));
        } else if (publicType != null) {
            return new GroovyRuntimeException(String.format(
                    "Cannot set the value of read-only property '%s' for object of type %s.", name, publicType.getName()));
        } else {
            // Use the display name anyway
            return new GroovyRuntimeException(String.format(
                    "Cannot set the value of read-only property '%s' for %s.", name, getDisplayName()));
        }
    }

    @Override
    public Map<String, ?> getProperties() {
        return Collections.emptyMap();
    }

    @Override
    public boolean hasMethod(String name, Object... arguments) {
        return false;
    }

    @Override
    public DynamicInvokeResult tryInvokeMethod(String name, Object... arguments) {
        return DynamicInvokeResult.notFound();
    }

    @Override
    public Object invokeMethod(String name, Object... arguments) throws groovy.lang.MissingMethodException {
        DynamicInvokeResult result = tryInvokeMethod(name, arguments);
        if (result.isFound()) {
            return result.getValue();
        }
        throw methodMissingException(name, arguments);
    }

    @Override
    public MissingMethodException methodMissingException(String name, Object... params) {
        Class<?> publicType = getPublicType();
        boolean includeDisplayName = hasUsefulDisplayName();
        final String message;
        if (publicType != null && includeDisplayName) {
            message = String.format("Could not find method %s() for arguments %s on %s of type %s.", name, Arrays.toString(params), getDisplayName(), publicType.getName());
        } else if (publicType != null) {
            message = String.format("Could not find method %s() for arguments %s on object of type %s.", name, Arrays.toString(params), publicType.getName());
        } else {
            // Include the display name anyway
            message = String.format("Could not find method %s() for arguments %s on %s.", name, Arrays.toString(params), getDisplayName());
        }
        // https://github.com/apache/groovy/commit/75c068207ba24648ea2d698c520601c6fcf0a45b
        return new CustomMissingMethodExecutionFailed(name, publicType, message, params);
    }

    private static class CustomMissingMethodExecutionFailed extends MissingMethodExecutionFailed {

        public CustomMissingMethodExecutionFailed(String name, Class<?> publicType, String message, Object... params) {
            super(name, publicType, params, false, new CustomMessageMissingMethodException(name, publicType, message, params));
        }

        @Override
        public String getMessage() {
            return getCause().getMessage();
        }
    }

    private static class CustomMessageMissingMethodException extends MissingMethodException {
        private final String message;

        CustomMessageMissingMethodException(String name, Class<?> publicType, String message, Object... params) {
            super(name, publicType, params);
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }
}