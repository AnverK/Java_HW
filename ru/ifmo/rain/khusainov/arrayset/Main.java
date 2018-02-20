package ru.ifmo.rain.khusainov.arrayset;


import java.util.*;

public class Main {

    public static void main(String[] args) {
        TreeSet<Integer> treeSet = new TreeSet<>();
        treeSet.add(1);
        treeSet.add(3);
        treeSet.add(2);

        ArraySet<Integer> arraySet = new ArraySet<Integer>(treeSet, Comparator.comparingInt(Integer::intValue).reversed());
        for (Integer integer: arraySet){
            System.out.println(integer);
        }
    }
}
