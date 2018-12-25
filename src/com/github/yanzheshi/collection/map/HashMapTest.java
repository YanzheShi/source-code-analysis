package com.github.yanzheshi.collection.map;

import java.util.Random;

/**
 * @author shiyanzhe
 * @date 2018/12/17
 */
public class HashMapTest {
    public static void main(String[] args) {
        resizeTest();
    }

    public static int hashMapHashTest() {
        Random random = new Random();
        int h = random.nextInt();
        System.out.println("origin hash: ");
        System.out.println(Integer.toBinaryString(h));
        System.out.println("Arithmetic shift right: ");
        System.out.println(Integer.toBinaryString(h >>> 16));
        System.out.println("new hash");
        System.out.println(Integer.toBinaryString(h ^ (h >>> 16)));
        return h ^ (h >>> 16);
    }

    public static void tableSizeForTest() {
        Random random = new Random();
        int cap = random.nextInt();
        cap = Math.abs(cap);
        System.out.println("origin hash: ");
        System.out.println(cap);
        System.out.println(Integer.toBinaryString(cap));
        int size = tableSizeFor(cap);
        System.out.println("size");
        System.out.println(size);
        System.out.println(Integer.toBinaryString(size));

    }

    public static void resizeTest() {
        int oldcap = 1 << 3;
        int newCap = oldcap << 1;

        Random random = new Random();
        int hash = Math.abs(random.nextInt());
        System.out.println("hash");
        System.out.println(hash);
        System.out.println(Integer.toBinaryString(hash));
        System.out.println("oldCap");
        System.out.println(oldcap);
        System.out.println(Integer.toBinaryString(oldcap));
        System.out.println("hash & oldCap");
        System.out.println(Integer.toBinaryString(hash & oldcap));

        System.out.println("old index:");
        int oldIndex = hash & (oldcap - 1);
        System.out.println(oldIndex);
        System.out.println(hash % (oldcap - 1));
        System.out.println(Integer.toBinaryString(oldIndex));
        int newIndex = hash & (newCap - 1);
        System.out.println("new index:");
        System.out.println(newIndex);
        System.out.println(Integer.toBinaryString(newIndex));
    }

    public static final int tableSizeFor(int cap) {
        int MAXIMUM_CAPACITY = 1 << 30;
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }
}
