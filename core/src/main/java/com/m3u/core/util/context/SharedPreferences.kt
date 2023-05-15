@file:Suppress("unused")

package com.m3u.core.util.context

import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun SharedPreferences.int(
    def: Int = 0,
    key: String? = null,
) = delegate(key, def, SharedPreferences::getInt, SharedPreferences.Editor::putInt)

fun SharedPreferences.intAsState(
    def: Int = 0,
    key: String
) = delegateAsState(key, def, SharedPreferences::getInt, SharedPreferences.Editor::putInt)

fun SharedPreferences.string(
    def: String? = null,
    key: String? = null
) = delegate(key, def, SharedPreferences::getString, SharedPreferences.Editor::putString)

fun SharedPreferences.stringAsState(
    def: String? = null,
    key: String
) = delegateAsState(key, def, SharedPreferences::getString, SharedPreferences.Editor::putString)

fun SharedPreferences.long(
    def: Long = 0,
    key: String? = null
) = delegate(key, def, SharedPreferences::getLong, SharedPreferences.Editor::putLong)

fun SharedPreferences.longAsState(
    def: Long = 0,
    key: String
) = delegateAsState(key, def, SharedPreferences::getLong, SharedPreferences.Editor::putLong)

fun SharedPreferences.boolean(
    def: Boolean = false,
    key: String
) = delegate(
    key,
    def,
    SharedPreferences::getBoolean,
    SharedPreferences.Editor::putBoolean
)

fun SharedPreferences.booleanAsState(
    def: Boolean = false,
    key: String
) = delegateAsState(
    key,
    def,
    SharedPreferences::getBoolean,
    SharedPreferences.Editor::putBoolean
)

private inline fun <T> SharedPreferences.delegate(
    key: String? = null,
    defaultValue: T,
    crossinline getter: SharedPreferences.(String, T) -> T,
    crossinline setter: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor
): ReadWriteProperty<Any, T> = object : ReadWriteProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>): T =
        getter(key ?: property.name, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) =
        edit().setter(key ?: property.name, value).apply()
}

private fun <T> SharedPreferences.delegateAsState(
    key: String,
    defaultValue: T,
    getter: SharedPreferences.(String, T) -> T,
    setter: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor
): MutableState<T> = StateDelegator(
    delegate = mutableStateOf(getter(key, defaultValue)),
    onUpdate = { edit().setter(key, it).apply() },
)

private class StateDelegator<T>(
    private val delegate: MutableState<T>,
    private val onUpdate: (T) -> Unit
) : MutableState<T> {
    override var value: T
        get() = delegate.value
        set(value) {
            onUpdate(value)
            delegate.value = value
        }

    override fun component1(): T = delegate.component1()

    override fun component2(): (T) -> Unit = delegate.component2()
}