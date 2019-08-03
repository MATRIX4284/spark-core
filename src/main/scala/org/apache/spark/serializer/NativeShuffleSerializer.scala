package org.apache.spark.serializer

import java.io.{IOException, _}
import java.nio.ByteBuffer

import scala.reflect.ClassTag

object NativeInputFormat {
  val KEY = 10
  val VALUE = 90
}


class NativeShuffleSerializer() extends Serializer with Serializable {
  override final def newInstance(): SerializerInstance = {
    NativeShuffleSerializerInstance.getInstance()
  }
  override lazy val supportsRelocationOfSerializedObjects: Boolean = true
}

class NativeShuffleSerializerInstance() extends SerializerInstance {

  override final def serialize[T: ClassTag](t: T): ByteBuffer = {
    throw new IOException("this call is not yet implemented : serializer[] ")
  }

  override final def deserialize[T: ClassTag](bytes: ByteBuffer): T = {
    throw new IOException("this call is not yet implemented : deserialize[]")
  }

  override final def deserialize[T: ClassTag](bytes: ByteBuffer, loader: ClassLoader): T = {
    throw new IOException("this call is not yet implemented : deserialize with classloader")
  }

  override final def serializeStream(o: OutputStream): SerializationStream = {
    new NativeShuffleSerializerStream(this, o)
  }

  override final def deserializeStream(i: InputStream): DeserializationStream = {
    new NativeShuffleDeSerializerStream(this, i)
  }
}

object NativeShuffleSerializerInstance {
  private var serIns:NativeShuffleSerializerInstance = null

  final def getInstance():NativeShuffleSerializerInstance= {
    this.synchronized {
      if(serIns == null)
        serIns = new NativeShuffleSerializerInstance()
    }
    serIns
  }
}

class NativeShuffleSerializerStream(explicitNativeShuffleSerializerInstance: NativeShuffleSerializerInstance,
                                    outStream: OutputStream) extends SerializationStream {


  override final def writeObject[T: ClassTag](t: T): SerializationStream = {
    /* explicit byte casting */
    val tmp = t.asInstanceOf[Array[Byte]]
    outStream.write(tmp, 0, tmp.length)
    this
  }

  override final def flush() {
    if (outStream == null) {
      throw new IOException("Stream is closed")
    }
    outStream.flush()
  }

  override final def writeKey[T: ClassTag](key: T): SerializationStream = writeObject(key)
  /** Writes the object representing the value of a key-value pair. */
  override final def writeValue[T: ClassTag](value: T): SerializationStream = writeObject(value)
  override final def close(): Unit = {
    if (outStream != null) {
      outStream.close()
    }
  }

  override final def writeAll[T: ClassTag](iter: Iterator[T]): SerializationStream = {
    while (iter.hasNext) {
      writeObject(iter.next())
    }
    this
  }
}

class NativeShuffleDeSerializerStream(explicitNativeShuffleSerializerInstance: NativeShuffleSerializerInstance,
                                      inStream: InputStream) extends DeserializationStream {

  override final def readObject[T: ClassTag](): T = {
    throw new IOException("this call is not yet implemented + readObject")
  }

  final def readBytes(bytes: Array[Byte]): Unit = {
    val ret = inStream.read(bytes, 0, bytes.length)
    if( ret < 0 ) {
      /* mark the end of the stream : this is spark's way off saying EOF */
      throw new EOFException()
    }
  }

  override final def readKey[T: ClassTag](): T = {
    val key = new Array[Byte](NativeInputFormat.KEY)
    readBytes(key)
    key.asInstanceOf[T]
  }

  override final def readValue[T: ClassTag](): T = {
    val value = new Array[Byte](NativeInputFormat.VALUE)
    readBytes(value)
    value.asInstanceOf[T]
  }

  override final def close(): Unit = {
    if (inStream != null) {
      inStream.close()
    }
  }
}

