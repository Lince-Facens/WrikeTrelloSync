package com.guichaguri.wriketrellosync.history;

import com.guichaguri.wriketrellosync.IBoardObject;

public interface IRelationHolder<T extends IBoardObject> extends IBoardObject {

    void copyFrom(T obj);

    boolean isEquals(T obj);

    String getId(String slug);

    void setId(String slug, String id);

}
