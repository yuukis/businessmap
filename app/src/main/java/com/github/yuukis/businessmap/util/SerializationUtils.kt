/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.yuukis.businessmap.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass
import java.io.OutputStream
import java.io.Serializable

/**
 * Assists with the serialization process and performs additional
 * functionality based on serialization: deep clone, and serialize/
 * deserialize while managing IOException internally.
 */
object SerializationUtils {

    // Clone
    // -----------------------------------------------------------------------
    /**
     * Deep clone a [Serializable] object using serialization.
     *
     * @throws SerializationException (runtime) if the serialization fails
     */
    @JvmStatic
    fun <T : Serializable> clone(`object`: T?): T? {
        if (`object` == null) {
            return null
        }
        val objectData = serialize(`object`)
        val bais = ByteArrayInputStream(objectData)

        var `in`: ClassLoaderAwareObjectInputStream? = null
        try {
            `in` = ClassLoaderAwareObjectInputStream(bais, `object`.javaClass.classLoader)
            @Suppress("UNCHECKED_CAST")
            return `in`.readObject() as T
        } catch (ex: ClassNotFoundException) {
            throw SerializationException("ClassNotFoundException while reading cloned object data", ex)
        } catch (ex: IOException) {
            throw SerializationException("IOException while reading cloned object data", ex)
        } finally {
            try {
                `in`?.close()
            } catch (ex: IOException) {
                throw SerializationException("IOException on closing cloned object data InputStream.", ex)
            }
        }
    }

    // Serialize
    // -----------------------------------------------------------------------
    /**
     * Serializes an [Object] to the specified stream.
     *
     * The stream will be closed once the object is written.
     *
     * @throws IllegalArgumentException if `outputStream` is null
     * @throws SerializationException (runtime) if the serialization fails
     */
    @JvmStatic
    fun serialize(obj: Serializable?, outputStream: OutputStream?) {
        if (outputStream == null) {
            throw IllegalArgumentException("The OutputStream must not be null")
        }
        var out: ObjectOutputStream? = null
        try {
            out = ObjectOutputStream(outputStream)
            out.writeObject(obj)
        } catch (ex: IOException) {
            throw SerializationException(ex)
        } finally {
            try {
                out?.close()
            } catch (ex: IOException) {
                // ignore close exception
            }
        }
    }

    /**
     * Serializes an [Object] to a byte array for storage/serialization.
     *
     * @throws SerializationException (runtime) if the serialization fails
     */
    @JvmStatic
    fun serialize(obj: Serializable?): ByteArray {
        val baos = ByteArrayOutputStream(512)
        serialize(obj, baos)
        return baos.toByteArray()
    }

    // Deserialize
    // -----------------------------------------------------------------------
    /**
     * Deserializes an [Object] from the specified stream.
     *
     * The stream will be closed once the object is written.
     *
     * @throws IllegalArgumentException if `inputStream` is null
     * @throws SerializationException (runtime) if the serialization fails
     */
    @JvmStatic
    fun deserialize(inputStream: InputStream?): Any? {
        if (inputStream == null) {
            throw IllegalArgumentException("The InputStream must not be null")
        }
        var `in`: ObjectInputStream? = null
        try {
            `in` = ObjectInputStream(inputStream)
            return `in`.readObject()
        } catch (ex: ClassNotFoundException) {
            throw SerializationException(ex)
        } catch (ex: IOException) {
            throw SerializationException(ex)
        } finally {
            try {
                `in`?.close()
            } catch (ex: IOException) {
                // ignore close exception
            }
        }
    }

    /**
     * Deserializes a single [Object] from an array of bytes.
     *
     * @throws IllegalArgumentException if `objectData` is null
     * @throws SerializationException (runtime) if the serialization fails
     */
    @JvmStatic
    fun deserialize(objectData: ByteArray?): Any? {
        if (objectData == null) {
            throw IllegalArgumentException("The byte[] must not be null")
        }
        val bais = ByteArrayInputStream(objectData)
        return deserialize(bais)
    }

    /**
     * Custom specialization of the standard JDK [ObjectInputStream] that
     * uses a custom `ClassLoader` to resolve a class. If the specified
     * `ClassLoader` is not able to resolve the class, the context
     * classloader of the current thread will be used.
     */
    private class ClassLoaderAwareObjectInputStream(
        `in`: InputStream,
        private val classLoader: ClassLoader?
    ) : ObjectInputStream(`in`) {

        @Throws(IOException::class, ClassNotFoundException::class)
        override fun resolveClass(desc: ObjectStreamClass): Class<*> {
            val name = desc.name
            return try {
                Class.forName(name, false, classLoader)
            } catch (ex: ClassNotFoundException) {
                Class.forName(name, false, Thread.currentThread().contextClassLoader)
            }
        }
    }
}
