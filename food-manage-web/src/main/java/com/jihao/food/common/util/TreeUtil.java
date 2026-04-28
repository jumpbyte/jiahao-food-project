package com.jihao.food.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class TreeUtil {

    private TreeUtil() {}

    /**
     * 将扁平列表转为树形结构
     */
    public static <T> List<T> buildTree(
            List<T> list,
            Function<T, Long> getId,
            Function<T, Long> getParentId,
            Function<T, List<T>> getChildren,
            BiConsumer<T, List<T>> setChildren,
            Predicate<T> rootFilter) {

        List<T> roots = new ArrayList<>();
        for (T item : list) {
            if (rootFilter.test(item)) {
                roots.add(item);
                buildChildren(item, list, getId, getParentId, getChildren, setChildren);
            }
        }
        return roots;
    }

    private static <T> void buildChildren(
            T parent,
            List<T> all,
            Function<T, Long> getId,
            Function<T, Long> getParentId,
            Function<T, List<T>> getChildren,
            BiConsumer<T, List<T>> setChildren) {

        List<T> children = new ArrayList<>();
        Long parentId = getId.apply(parent);
        for (T item : all) {
            if (parentId.equals(getParentId.apply(item))) {
                children.add(item);
                buildChildren(item, all, getId, getParentId, getChildren, setChildren);
            }
        }
        setChildren.accept(parent, children);
    }
}
