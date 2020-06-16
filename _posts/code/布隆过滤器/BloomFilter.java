package com;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class BloomFilter {

    // 布隆过滤器长度
    private static final int SIZE = 2 << 10;
    // 模拟实现不同的哈希函数
    private static final int[] num= new int[] {5, 19, 23, 31,47, 71};
    // 初始化位数组
    private BitSet bits = new BitSet(SIZE);
    // 用于存储哈希函数
    private MyHash[] function = new MyHash[num.length];
    // 初始化哈希函数
    public BloomFilter() {
        for (int i = 0; i < num.length; i++) {
            function [i] = new MyHash(SIZE, num[i]);
        }
    }

    // 存值Api
    public void add(String value) {
        // 对存入得值进行哈希计算
        for (MyHash f: function) {
            // 将为数组对应的哈希下标得位置得值改为1
            bits.set(f.hash(value), true);
        }
    }

    // 判断是否存在该值得Api
    public boolean contains(String value) {
        if (value == null) {
            return false;
        }
        boolean result= true;
        for (MyHash f : function) {
            result= result&& bits.get(f.hash(value));
        }
        return result;
    }

    public static class MyHash {

        //容量
        private int cap;
        //哈希因子
        private int seed;
        // 初始化数据
        public MyHash(int cap, int seed) {
            this.cap = cap;
            this.seed = seed;
        }
        // 哈希函数
        public int hash(String value) {
            int result = 0;
            int len = value.length();
            for (int i = 0; i < len; i++) {
                result = seed * result + value.charAt(i);
            }
            return (cap - 1) & result;
        }
    }


    public static void main(String[] args) {
        int i=0;
        int bt=0;
        int st=0;
        BloomFilter filter = new BloomFilter();
        Set<Integer> set = new HashSet<>();
        while (i<10000){
            Random random = new Random();
            int a = random.nextInt();
            //boolean b = filter.contains(String.valueOf(a));
            com.google.common.hash.BloomFilter guava = com.google.common.hash.BloomFilter.create(Funnels.integerFunnel(), 1);
            boolean b = guava.mightContain(a);
            if(b){
                bt++;
            }
            if((b&&set.contains(a))||(!b&&!set.contains(a))){
                //判断正确的数量
                st ++;
            }
//            System.out.println(b + ":" +a);
            //filter.add(String.valueOf(a));
            guava.put(a);
            set.add(a);
            i++;
        }
        System.out.println("布隆判断存在的数据量："+bt);
        System.out.println("布隆判断判断正确的数量："+st);
        System.out.println("样本总数:"+set.size());
    }
}
