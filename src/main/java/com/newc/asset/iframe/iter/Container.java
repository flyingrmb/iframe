package com.newc.asset.iframe.iter;

import com.alibaba.fastjson.JSONObject;
import com.newc.asset.iframe.entity.Identity;
import com.newc.asset.iframe.entity.ZipSide;
import com.newc.asset.iframe.util.Reflection;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.newc.asset.iframe.util.IdentityBuilder.MetaIdentity;

/**
 * Created by paul on 2018/5/10.
 */
public class Container {
    private final JSONObject model; // The origin model object.
    // The memory space used to store all the nodes.
    private final Map<Identity, Extension> extensions;
    private final Map<String, Set<Identity>> iterators;

    public Container(JSONObject model) {
        Assert.notNull(model, "model should not be null.");

        this.model = model;
        this.extensions = new ConcurrentHashMap<Identity, Extension>();
        this.iterators = new ConcurrentHashMap<String, Set<Identity>>();
    }

    public JSONObject getModel() {
        return model;
    }

    /**
     * This will return all the Identities of the special node with path equals `path`
     * @param path
     * @return
     */
    public Set<Identity> getIdentities(String path) {
        Assert.notNull(path, "the path should not be null");
        return iterators.get(path);
    }

    public Set<Object> search(String path, Identity kvs) {
        return search(path, kvs, false);
    }

    // When append is false, we will add a node in model if we did not find anything.
    public Set<Object> search(String path, Identity kvs, boolean append) {
        Assert.notNull(path, "path should not be null.");
        Assert.notNull(kvs, "kvs should not be null.");
        Set<Object> result = get(path, kvs);
        if (result != null) return result;

        // result == null this means we have not traversal the model with `path` and `kvs`
        // So let's do it now
        update(path, kvs.unzip(ZipSide.Left), append);

        // and now, we try to get the result again for the reason that we have constructed it again.
        return get(path, kvs);
    }

    /**
     * return null means we have never traversal the model with this `path` and `kvs`
     * return empty list means we have traversal the model with this `path` and `kvs' but nothing has been found.
     * @param path
     * @param kvs
     * @return
     */
    private Set<Object> get(String path, Identity kvs) {
        Identity values = kvs.unzip(ZipSide.Right);
        Extension extension = extensions.get(values);
        if (extension == null) return null;

        return extension.get(path, kvs);
    }

    public void update(String path, Identity names) {
        update(path, names, false); // Do not add anything new in the model.
    }

    public void update(String path, Identity names, boolean append) {
        Assert.notNull(path, "path should not be null.");
        Assert.notNull(names, "names should not be null.");

        List<Object> total = Reflection.getAll(model, path);
        Assert.notNull(total, "Total should never be null.");

        // This means we did not find anything in the path
        if (total.isEmpty()) {
            if (append) { // This means that we want to add a node in the model when we find nothing
                Object added = Reflection.search(model, path);
                total.add(added);
            }
        }

        for (Object element : total) {
            updateGroup(path, MetaIdentity, element); // update the all category.

            if (names != MetaIdentity) {
                Identity values = names.values(element);
                updateGroup(path, names.zip(values), element);
            }
        }
    }

    private void updateGroup(String path, Identity kvs, Object element) {
        Identity names = kvs.unzip(ZipSide.Left);
        Identity values = kvs.unzip(ZipSide.Right);

        Extension extension = searchExtension(values);
        Set<Identity> iterator = searchIterator(path);
        if (kvs.matches(element)) {
            extension.add(path, names, element);
            iterator.add(kvs); // Save the key-value Identity.
        }
    }

    private Extension searchExtension(Identity values) {
        Extension extension = extensions.get(values);
        if (extension != null) return extension;

        extensions.put(values, new Extension(model, values));
        return extensions.get(values);
    }

    private Set<Identity> searchIterator(String path) {
        Set<Identity> iterator = iterators.get(path);
        if (iterator != null) return iterator;

        iterators.put(path, new HashSet<Identity>());
        return iterators.get(path);
    }
}
