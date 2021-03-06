package net.ahwater.zjk.utils;

import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by yqz on 2020/5/27
 */
@SuppressWarnings("all")
@Component
public class DozerUtil{

    @Autowired
    protected Mapper dozerMapper;

    public <T,S> T convert(S s,Class<T> clz){
        if(s == null){
            return null;
        }
        return this.dozerMapper.map(s,clz);
    }

    public <T,S> List<T> convert(List<S> s,Class<T> clz){
        if(s == null){
            return null;
        }
        List<T> list = new ArrayList<>();
        for(S vs : s){
            list.add(this.dozerMapper.map(vs,clz));
        }
        return list;
    }

    public <T,S> Set<T> convert(Set<S> s, Class<T> clz){
        if(s == null){
            return null;
        }
        Set<T> set = new HashSet<>();
        for(S vs : s){
            set.add(this.dozerMapper.map(vs,clz));
        }
        return set;
    }

    public <T,S> T[] covert(S[] s,Class<T> clz){
        if(s == null){
            return null;
        }
        T[] arr = (T[]) Array.newInstance(clz,s.length);
        for(int i = 0; i < s.length; i++){
            arr[i] = this.dozerMapper.map(s[i],clz);
        }
        return arr;
    }
}