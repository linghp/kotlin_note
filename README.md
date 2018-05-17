kotlin_note
===
kotlin的一些操作符号
---
1. 展开运算符'*'   
首先看可变参数 vararg的使用：
```kotlin
fun kebiancanshu() {
    val list = listOf(1, 45, 36)
　　 //listOf源码
    //public fun <T> listOf(vararg elements: T): List<T> = if (elements.size > 0) elements.asList() else emptyList()
}
```
* 上面可以看在Kotlin使用了 vararg 代替了 Java中的... 声明了可变参数
* 与Java不用的一点是如果在传入参数是已经包装在数组中的参数时，在Java中会原样传递数组，而Kotlin则要求你显示着解包数组，以便于每个数组元素能作为单独的参数来调用
* 从技术角度讲这个功能被称为"展开运算符"，而在使用的时候不过是在参数前面放一个 “*”
```kotlin
fun testVararg(args: Array<String>) {
    val list = listOf("test", * args)
    println(list)
}
```
