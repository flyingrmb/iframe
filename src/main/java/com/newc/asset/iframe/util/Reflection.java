package com.newc.asset.iframe.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by paul on 2018/5/9.
 */
public class Reflection {
    public static Log logger = LogFactory.getLog(Reflection.class);

    public static Object search(Object object, String signature) {
        Path path = new Path(signature);
        Route route = new Route(object, path.getPath(), true);

        return route.search(path.getKey());
    }

    public static Object get(Object object, String signature) {
        Path path = new Path(signature);
        Route route = new Route(object, path.getPath());

        return route.get(path.getKey());
    }

    /**
     * We name this method as set because we don't want to override the JSONObject's put method.
     * @param signature
     * @param value
     */
    public static void set(Object object, String signature, Object value) {
        Path path = new Path(signature);
        Route route = new Route(object, path.getPath());

        route.put(path.getKey(), value);
    }

    public static List<Object> getAll(Object object, String signature) {
        Path path = new Path(signature);
        Route route = new Route(object, path.getPath());

        return route.getAll(path.getKey());
    }

    public static Object getChild(Object object, String key) {
        return getChild(object, key, false);
    }

    private static Object getChild(Object object, String key, boolean append) {
        Method method = ReflectionUtils.findMethod(object.getClass(), "get", Object.class);
        if (method == null) {
            logger.warn("Could not find `get` method for key: " + key + " of object: " + object);
            return null;
        }

        Object result = ReflectionUtils.invokeMethod(method, object, key);
        if (append == false) return result;

        if (result != null) return result;
        // As we have not found the child with name of `key`, so we should new one with JSONObject type and add it to the `object`
        result = new JSONObject();
        method = ReflectionUtils.findMethod(object.getClass(), "put", Object.class, Object.class);
        ReflectionUtils.invokeMethod(method, object, key, result);

        return result;
    }

    private static final class Path {
        private String path; // The path to the leaf element
        private String key; // The leaf element key

        public Path(String signature) {
            Assert.notNull(signature, "Signature should not be null.");
            int index = signature.lastIndexOf('.'); // Find the last index of split dot.
            if (index == -1) { // This means we want just pick up element from the root node.
                this.path = ""; // Does not have any path to leaf element, we use empty string so won't get NullPointException;
                this.key = signature; // signature is equals to the key for root.
                return ;
            }

            this.path = signature.substring(0, index).trim();
            this.key = signature.substring(index + 1).trim();

            // Make sure the dot is not the first or the last char in signature, eg: .key or key., this is illegal.
            Assert.isTrue(this.path.length() > 0, "The dot(.) is the first element of signature, it is illegal.");
            Assert.isTrue(this.key.length() > 0, "The dot(.) is the last element of signature, it is illegal.");
        }

        public boolean isRoot() {
            return this.path.length() == 0;
        }

        public String[] getPath() {
            return path.split("\\.");
        }

        public String getKey() {
            return key;
        }
    }

    /**
     * 遍历器，根据path定位到对应的对象
     */
    private static final class Route {
        private static final Log logger = LogFactory.getLog(Route.class);


        private Object object = null;
        private String[] path = null;
        private List<Object> leaf = null;

        public Route(Object object, String[] path) {
            this(object, path, false);
        }

        public Route(Object object, String[] path, boolean append) {
            Assert.notNull(object, "entity should not be null.");
            Assert.notNull(path, "path should not be null.");

            this.object = object;
            this.path = path;

            routeToLeaf(append);
        }

        private void routeToLeaf(boolean append) {
            List<Object> current = new ArrayList<Object>();
            current.add(object);

            for (int i=0; i<path.length; i++)
                current = retrieve(current, path[i], append);

            leaf = current;
        }

        private List<Object> retrieve(List<Object> current, String key, boolean append) {
            if (key.length() == 0) {
                /**
                 * For the reason that split method may return items with value of "",
                 * so we just understand blank string as ignore this node and just return the origin list for chaining.
                 */
                return current;
            }

            List<Object> result = new ArrayList<Object>();
            for (Object item : current) {
                Object children = getChild(item, key);
                result = mergeChildrenToResult(result, children);
            }

            return result;
        }

        private List<Object> mergeChildrenToResult(List<Object> result, Object children) {
            if (children == null) return result;

            if (!(children instanceof List)) {
                result.add(children);
                return result;
            }

            result.addAll((List)children);
            return result;
        }

        public Object search(String key) {
            Assert.notNull(leaf, "The property of leaf should not be null.");
            if (leaf.size() == 0) {
                logger.debug("Any does not contains correct node.");
                return null;
            }

            List<Object> collection = get(leaf.get(0), key);
            if (collection == null || collection.size() == 0)
                put(key, new JSONObject());

            collection = get(leaf.get(0), key);
            return collection.get(0);
        }

        public Object get(String key) {
            Assert.notNull(leaf, "The property of leaf should not be null.");
            if (leaf.size() == 0) {
                logger.debug("Any does not contains correct node.");
                return null;
            }

            List<Object> collection = get(leaf.get(0), key);
            if (collection == null || collection.size() == 0) return null;

            return collection.get(0); // Return the first element from the collection.
        }

        public List<Object> getAll(String key) {
            List<Object> result = new ArrayList<Object>();

            if (leaf.size() == 0) {
                logger.debug("Any does not contains correct node.");
                return null;
            }

            for (Object object : leaf) {
                List<Object> collection = get(object, key);
                result.addAll(collection);
            }

            return result;
        }

        /**
         * The reason that we design this method with List return type is because we may get an array
         * using object.get(key)
         * @param object
         * @param key
         * @return
         */
        private List<Object> get(Object object, String key) {
            List<Object> result = new ArrayList<Object>();

            Method method = ReflectionUtils.findMethod(object.getClass(), "get", Object.class);
            if (method == null) {
                logger.error("The first element of leaf does not contains `get` method, element type: " + object.getClass());
                return result;
            }

            Object item = ReflectionUtils.invokeMethod(method, object, key);
            if (item == null) return result;

            if (!(item instanceof List)) {
                result.add(item);
                return result;
            }

            // dis-package all the elements in List
            result.addAll((List)item);
            return result;
        }

        public void put(String key, Object value) {
            Assert.notNull(leaf, "The property leaf should not be null");
            Assert.notEmpty(leaf, "The property leaf should not be empty");

            // This assertion is just for simplifying the design difficulty,
            // we now just require the leaf can only contains one element.
            Assert.isTrue(leaf.size() == 1, "Only the exact one element can be add new value.");

            Object target = leaf.get(0);
            Method method = ReflectionUtils.findMethod(target.getClass(), "put", Object.class, Object.class);
            ReflectionUtils.invokeMethod(method, target, key, value);
        }
    }
}
