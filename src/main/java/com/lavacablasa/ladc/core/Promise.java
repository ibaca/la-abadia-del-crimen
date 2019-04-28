package com.lavacablasa.ladc.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Promise<T> {

    public static <T> Promise<T> of(T val) { return new Promise<T>().resolve(val);}
    public static Promise<Void> done() { return of(null);}

    public static Promise<Object[]> merge(Promise<?>... ps) {
        Promise<Object[]> out = new Promise<>();
        Boolean[] resolved = new Boolean[ps.length];
        Object[] outVals = new Object[ps.length];
        for (int i = 0; i < ps.length; i++) {
            Promise<?> p = ps[i];
            int I = i;
            p.subscribe(n -> {
                resolved[I] = true;
                outVals[I] = n;
                if (Stream.of(resolved).allMatch(r -> r)) out.resolve(outVals);
            });
        }
        return out;
    }

    public static Promise<Boolean> doWhile(Supplier<Promise<Boolean>> fn) {
        return doWhile(true, n -> n, n -> fn.get());
    }
    public static <T> Promise<T> doWhile(T val, Predicate<T> test, Function<T, Promise<T>> fn) {
        Promise<T> out = new Promise<>();
        fn.apply(val).subscribe(new Consumer<>() {
            @Override public void accept(T n) {
                if (test.test(n)) fn.apply(n).subscribe(this);
                else out.resolve(n);
            }
        });
        return out;
    }

    private boolean resolved = false;
    private T val;
    private List<Consumer<T>> subscribers = new ArrayList<>(1);

    private void subscribe(Consumer<T> next) {
        if (resolved) next.accept(val);
        else subscribers.add(next);
    }
    Promise<T> resolve(T t) {
        resolved = true;
        val = t;
        for (Consumer<T> then : subscribers) then.accept(t);
        subscribers.clear();
        return this;
    }

    public <V> Promise<V> map(V fn) {
        var out = new Promise<V>();
        subscribe(t -> out.resolve(fn));
        return out;
    }
    public <V> Promise<V> map(Function<T, V> fn) {
        var out = new Promise<V>();
        subscribe(t -> out.resolve(fn.apply(t)));
        return out;
    }

    public Promise<T> andThen(Runnable fn) {
        var out = new Promise<T>(); //@formatter:off
        subscribe(t -> { fn.run(); out.resolve(t); });
        return out; //@formatter:on
    }
    public <V> Promise<V> andThen(Supplier<Promise<V>> fn) {
        var out = new Promise<V>();
        subscribe(t -> fn.get().subscribe(out::resolve));
        return out;
    }
    public <V> Promise<V> andThen(Function<T, Promise<V>> fn) {
        var out = new Promise<V>();
        subscribe(t -> fn.apply(t).subscribe(out::resolve));
        return out;
    }
}
