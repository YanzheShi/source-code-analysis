# HashMap jdk8

## 实现原理

HashMap是通过一个包含key和value的Node数组来进行存储的。通过Hash定位数组下标
从而快速查找。如果存在Hash冲突，首先使用链地址法解决，即在冲突位置形成一条单向链表。
查找时，首先根据Hash定位到数组位置，地址为散列值与表的最大下标按位与的结果（hash &
(cap - 1)）然后比较key是否相等，如果不想等就依次遍历链表，
直到key相等。当链表的长度大于或等于8的时候，将冲突节点转化为红黑树，从而提高了查找
效率。

## 组成类

- HashMap.Node

用来存储key-value的数组，除了key，value外，还有指向下一个节点的引用next。

- HashMap.TreeNode

是红黑树的一个节点, 除了key，value外，包含了节点的左子节点left，右子节点right，父节点
parent，链表前置节点prev

## 常量

- DEFAULT_INITIAL_CAPACITY

默认初始容量16，每次扩大两倍

- MAXIMUM_CAPACITY

最大容量 1 << 30

- DEFAULT_LOAD_FACTOR

默认装填银子0.75， map 中的node个数与容量的比值如果大于装填因子，则扩容

- TREEIFY_THRESHOLD

链表转化为红黑树的一个条件为：size >= 8

- UNTREEIFY_THRESHOLD

从红黑树转化为链表的门槛为：size <= 6

- MIN_TREEIFY_CAPACITY

链表转化为红黑树的另一个条件是：capacity >= 64

## 域

- table

Node数组，用来存放key-value

- size

    key-value的总个数

- **threshold**

    下次需要扩容的size大小

- loadFactor

    实际设置的装填因子， 用来决定扩容的时机

## 方法

### 求key散列值方法 hash

```java
        static final int hash(Object key) {
            int h;
            return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
        }
```

对key的原始hash值进行了加工，高16位不变，低16位取高16位与低16的异或值。由于根据
hash计算地址是利用取模运算，所以其决定作用的是低位，所以提高低位的散列度能减少地址碰撞。

### 根据给定容量计算表的容量

```java
        static final int tableSizeFor(int cap) {
            int n = cap - 1;
            n |= n >>> 1;
            n |= n >>> 2;
            n |= n >>> 4;
            n |= n >>> 8;
            n |= n >>> 16;
            return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
        }

```

由于要求表的大小必须是2的指数幂，而设置的容量cap可能不满足，所以需要根据cap计算出合适的表大小

### 扩容

该方法有两种功能， 一种是初始化table，另一种是将table扩容两倍： 申请大小为原来两倍的数组，将旧node
存到新的节点上。

```java
    final Node<K,V>[] resize() {
        Node<K,V>[] oldTab = table;
        // 扩容前容量
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        // 保存旧的threshold（扩容门槛）
        int oldThr = threshold;
        int newCap, newThr = 0;
        if (oldCap > 0) {
            if (oldCap >= MAXIMUM_CAPACITY) {
                //已经扩容到极限了，不再扩容
                threshold = Integer.MAX_VALUE;
                return oldTab;
            }
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                     oldCap >= DEFAULT_INITIAL_CAPACITY)
                //容量扩大两倍，新的扩容阈值也要扩大两倍
                newThr = oldThr << 1; // double threshold
        }
        // table == null的情况, 对table和相关变量进行初始化
        else if (oldThr > 0) // initial capacity was placed in threshold
            newCap = oldThr;
        else {               // zero initial threshold signifies using defaults
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                      (int)ft : Integer.MAX_VALUE);
        }
        threshold = newThr;
        @SuppressWarnings({"rawtypes","unchecked"})
            Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
        table = newTab;
        if (oldTab != null) {
            //循环将旧表复制到新表中
            for (int j = 0; j < oldCap; ++j) {
                Node<K,V> e;
                if ((e = oldTab[j]) != null) {
                    // **!防止内存泄漏**
                    oldTab[j] = null;
                    if (e.next == null)
                        newTab[e.hash & (newCap - 1)] = e;
                    else if (e instanceof TreeNode)
                        // 处理冲突节点是红黑树的情况，由于扩容，根据hash
                        // 计算得到的地址分两种情况，一种与旧地址相同，另一种为
                        // 旧地址加上旧容量。   原因下面会说。将这两种情况的节点先拉链，然后根据
                        // 阈值去判断是拉链还是组成红黑树。
                        ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                    else { // preserve order
                        // 处理拉链的情况, 分两种情况
                        // 1 hash值与旧容量按位与结果为0： e.hash & oldCap == 0 这时候元素在
                        // 新表的位置与当前一致
                        // 2 否则  由于新容量是旧容量的2倍（左移一位）这时候元素在新表的位置为
                        // 当前位置+oldCap  
                        // 如果转化为二进制后观察，很容易发现原因
                        Node<K,V> loHead = null, loTail = null;
                        Node<K,V> hiHead = null, hiTail = null;
                        Node<K,V> next;
                        do {
                            // 将两种情况分别组成新链表
                            next = e.next;
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            }
                            else {
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        // 将新链表放在对应位置上
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        return newTab;
    }
```

### **添加键值对**

如果key存在，将对应的value修改为新值，返回旧值，否则插入新的key-value，返回null。 如果hash冲突， 首先将冲突节点通过链地址法进行拉链， 如果冲突的节点
个数大于等于TREEIFY_THRESHOLD（8）， 则将链表转化为红黑树

```java
    public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }

    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        Node<K,V>[] tab; Node<K,V> p; int n, i;
        if ((tab = table) == null || (n = tab.length) == 0)
            // 如果表未初始化， 调用resieze进行初始化
            n = (tab = resize()).length;
        // 地址计算方式是 (n - 1) & hash, 及table的最大下标与hash按位与
        if ((p = tab[i = (n - 1) & hash]) == null)
            // 对应位置没有元素的情况，直接填入
            tab[i] = newNode(hash, key, value, null);
        else {
            // 对应位置有元素的情况
            // 出现地址冲突，需要进行拉链或者在红黑树中增加节点
            Node<K,V> e; K k;
            // 用e记录key对应位置的旧的节点
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                // 第一个节点即为查找的节点
                e = p;
            else if (p instanceof TreeNode)
                // 节点是红黑树类型，调用红黑树的查找方法查找key对应的旧节点，如果没有则插入新节点
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
            else {
                // 遍历单链表并设置新值
                for (int binCount = 0; ; ++binCount) {
                    if ((e = p.next) == null) {
                        //key不存在，插入新节点
                        p.next = newNode(hash, key, value, null);
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            // 链表大小大于或等于8， 转化为红黑树
                            treeifyBin(tab, hash);
                        break;
                    }
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        break;
                    // key存在，返回旧节点
                    p = e;
                }
            }
            if (e != null) { // existing mapping for key
                V oldValue = e.value;
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                afterNodeAccess(e);
                return oldValue;
            }
        }
        ++modCount;
        if (++size > threshold)
            resize();
        afterNodeInsertion(evict);
        return null;
    }

```

### 根据key获取value

根据key计算地址，然后返回对应地址node的value，如果存在hash冲突， 首先判断冲突节点类型， 然后遍历链表
或红黑树获取对应的value，如果key不存在，返回null

```java
    public V get(Object key) {
        Node<K,V> e;
        return (e = getNode(hash(key), key)) == null ? null : e.value;
    }

    final Node<K,V> getNode(int hash, Object key) {
        Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (first = tab[(n - 1) & hash]) != null) {
            if (first.hash == hash && // always check first node
                ((k = first.key) == key || (key != null && key.equals(k))))
                //对应地址上第一个节点即为所得
                return first;
            // 存在hash冲突， 根据node类型进行查找
            if ((e = first.next) != null) {
                if (first instanceof TreeNode)
                    return ((TreeNode<K,V>)first).getTreeNode(hash, key);
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
    }

```

### 将链表转化为红黑树

将链表转化为红黑树. 转化为红黑树的条件有两个，一个是hash冲突的节点个数大于等于8，
另一个是**table**大小大于或等于64

```java
    final void treeifyBin(Node<K,V>[] tab, int hash) {
        int n, index; Node<K,V> e;
        if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
            // 不满足建树条件， 先初始化或扩容
            resize();
        else if ((e = tab[index = (n - 1) & hash]) != null) {
            TreeNode<K,V> hd = null, tl = null;
            // 循环将Node类型转化为TreeNode， 并记录顺序
            do {
                TreeNode<K,V> p = replacementTreeNode(e, null);
                if (tl == null)
                    hd = p;
                else {
                    p.prev = tl;
                    tl.next = p;
                }
                tl = p;
            } while ((e = e.next) != null);
            // 构建红黑树
            if ((tab[index] = hd) != null)
                hd.treeify(tab);
        }
    }

        final void treeify(Node<K,V>[] tab) {
            TreeNode<K,V> root = null;
            for (TreeNode<K,V> x = this, next; x != null; x = next) {
                next = (TreeNode<K,V>)x.next;
                x.left = x.right = null;
                if (root == null) {
                    x.parent = null;
                    x.red = false;
                    root = x;
                }
                else {
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    for (TreeNode<K,V> p = root;;) {
                        int dir, ph;
                        K pk = p.key;
                        if ((ph = p.hash) > h)
                            dir = -1;
                        else if (ph < h)
                            dir = 1;
                        else if ((kc == null &&
                                  (kc = comparableClassFor(k)) == null) ||
                                 (dir = compareComparables(kc, k, pk)) == 0)
                            dir = tieBreakOrder(k, pk);

                        TreeNode<K,V> xp = p;
                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
                            x.parent = xp;
                            if (dir <= 0)
                                xp.left = x;
                            else
                                xp.right = x;
                            root = balanceInsertion(root, x);
                            break;
                        }
                    }
                }
            }
            moveRootToFront(tab, root);
        }


```
