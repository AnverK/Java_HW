package ru.ifmo.rain.khusainov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> threads;
    private final Queue<Runnable> jobs;

    public ParallelMapperImpl(int threads) {
        this.threads = new ArrayList<>(threads);
        jobs = new ArrayDeque<>();

        Runnable runnable = () -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Runnable head;
                    synchronized (jobs) {
                        while (jobs.isEmpty()) {
                            jobs.wait();
                        }
                        head = jobs.poll();
                    }
                    head.run();
                }
            } catch (InterruptedException ignored) {
            } finally {
                Thread.currentThread().interrupt();
            }
        };

        for (int i = 0; i < threads; i++) {
            this.threads.add(new Thread(runnable));
                this.threads.get(i).start();
        }
    }

    private class MutableInteger {
        private int value = 0;

        int getValue() {
            return value;
        }

        void increment() {
            value++;
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        final List<R> result = new ArrayList<>(Collections.nCopies(args.size(), null));

        final MutableInteger counter = new MutableInteger();

        for (int i = 0; i < args.size(); i++) {
            final int index = i;
            Runnable runnable = () -> {
                result.set(index, f.apply(args.get(index)));
                synchronized (counter) {
                    counter.increment();
                    if (counter.getValue() == args.size()) {
                        counter.notify();
                    }
                }
            };
            synchronized (jobs) {
                jobs.add(runnable);
                jobs.notify();
            }
        }

        synchronized (counter) {
            while (counter.getValue() < args.size()) {
                counter.wait();
            }
        }
        return result;
    }

    @Override
    public void close() {
        threads.forEach(Thread::interrupt);
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }
}
