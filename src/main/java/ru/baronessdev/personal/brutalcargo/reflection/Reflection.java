package ru.baronessdev.personal.brutalcargo.reflection;

import com.google.common.primitives.Primitives;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.block.Block;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressWarnings("rawtypes")
public class Reflection {

    static {
        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Compares two arrays of classes
     *
     * @param l1 - The first array of classes
     * @param l2 - The second array of classes
     * @return True if the classes matches in the 2 arrays, false otherwise
     */
    public static boolean classArrayCompare(Class[] l1, Class[] l2) {
        if (l1.length != l2.length) {
            return false;
        }
        for (int i = 0; i < l1.length; i++) {
            if (l1[i] != l2[i])
                return false;
        }
        return true;
    }

    /**
     * Compares two arrays of classes
     *
     * @param l1 - The first array of classes
     * @param l2 - The second array of classes
     * @return True if each of the second arrays classes is assignable from the first arrays classes
     */
    public static boolean classArrayCompareLight(Class[] l1, Class[] l2) {
        if (l1.length != l2.length) {
            return false;
        }
        for (int i = 0; i < l1.length; i++) {
            if (!Primitives.wrap(l2[i]).isAssignableFrom(Primitives.wrap(l1[i])))
                return false;
        }
        return true;
    }

    public static Method getMethod(Class<org.bukkit.block.Block> cl, String name, Class... args) {
        if (cl == null || name == null || ArrayUtils.contains(args, null))
            return null;

        if (args.length == 0) {
            while (cl != null) {
                Method m = methodCheckNoArg(cl, name);
                if (m != null) {
                    m.setAccessible(true);
                    return m;
                }
                cl = (Class<Block>) cl.getSuperclass();
            }
        } else {
            while (cl != null) {
                Method m = methodCheck(cl, name, args);
                if (m != null) {
                    m.setAccessible(true);
                    return m;
                }
                cl = (Class<Block>) cl.getSuperclass();
            }
            StringBuilder sb = new StringBuilder();
            for (Class c : args)
                sb.append(", ").append(c.getName());
        }
        return null;
    }

    private static Method methodCheck(Class<org.bukkit.block.Block> cl, String name, Class[] args) {
        try {
            return cl.getDeclaredMethod(name, args);
        } catch (Throwable e) {
            Method[] mtds = cl.getDeclaredMethods();
            for (Method met : mtds)
                if (classArrayCompare(args, met.getParameterTypes()) && met.getName().equals(name))
                    return met;
            for (Method met : mtds)
                if (classArrayCompareLight(args, met.getParameterTypes()) && met.getName().equals(name))
                    return met;
            for (Method met : mtds)
                if (classArrayCompare(args, met.getParameterTypes()) && met.getName().equalsIgnoreCase(name))
                    return met;
            for (Method met : mtds)
                if (classArrayCompareLight(args, met.getParameterTypes()) && met.getName().equalsIgnoreCase(name))
                    return met;
            return null;
        }
    }

    private static Method methodCheckNoArg(Class<org.bukkit.block.Block> cl, String name) {
        try {
            return cl.getDeclaredMethod(name);
        } catch (Throwable e) {
            Method[] mtds = cl.getDeclaredMethods();
            for (Method met : mtds)
                if (met.getParameterTypes().length == 0 && met.getName().equalsIgnoreCase(name))
                    return met;
            return null;
        }
    }
}
