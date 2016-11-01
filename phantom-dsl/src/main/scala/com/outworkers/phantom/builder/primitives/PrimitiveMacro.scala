/*
 * Copyright 2013 - 2017 Outworkers Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.outworkers.phantom.builder.primitives

import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.{Date, UUID}

import macrocompat.bundle
import org.joda.time.DateTime

import scala.language.experimental.macros

@bundle
class PrimitiveMacro(val c: scala.reflect.macros.blackbox.Context) {
  import c.universe._

  val rowType = tq"com.datastax.driver.core.GettableData"
  val boolType = tq"scala.Boolean"
  val strType: Tree = tq"java.lang.String"
  val intType: Tree = tq"scala.Int"
  val byteType: Tree = tq"scala.Byte"
  val doubleType: Tree = tq"scala.Double"
  val shortType: Tree = tq"scala.Short"
  val uuidType: Tree = tq"java.util.UUID"
  val longType: Tree = tq"scala.Long"
  val floatType: Tree = tq"scala.Float"
  val dateType: Tree = tq"java.util.Date"
  val localDate: Tree = tq"com.datastax.driver.core.LocalDate"
  val dateTimeType: Tree = tq"org.joda.time.DateTime"
  val localJodaDate: Tree = tq"org.joda.time.LocalDate"
  val bigDecimalType: Tree = tq"scala.math.BigDecimal"
  val inetType: Tree = tq"java.net.InetAddress"
  val bigIntType = tq"scala.math.BigInt"
  val bufferType = tq"java.nio.ByteBuffer"

  val cql = q"com.outworkers.phantom.builder.query.CQLQuery"
  val syntax = q"com.outworkers.phantom.builder.syntax.CQLSyntax"

  val prefix = q"com.outworkers.phantom.builder.primitives"

  def tryT(x: Tree): Tree = tq"scala.util.Try[$x]"

  def typed[A : c.WeakTypeTag]: Symbol = weakTypeOf[A].typeSymbol

  object Symbols {
    val intSymbol = typed[Int]
    val byteSymbol = typed[Byte]
    val stringSymbol = typed[String]
    val boolSymbol = typed[Boolean]
    val shortSymbol = typed[Short]
    val longSymbol = typed[Long]
    val doubleSymbol = typed[Double]
    val floatSymbol = typed[Float]
    val dateSymbol = typed[Date]
    val listSymbol = typed[scala.collection.immutable.List[_]]
    val setSymbol = typed[scala.collection.immutable.Set[_]]
    val mapSymbol = typed[scala.collection.immutable.Map[_, _]]
    val dateTimeSymbol = typed[DateTime]
    val localDateSymbol = typed[com.datastax.driver.core.LocalDate]
    val uuidSymbol = typed[UUID]
    val jodaLocalDateSymbol = typed[org.joda.time.LocalDate]
    val inetSymbol = typed[InetAddress]
    val bigInt = typed[BigInt]
    val bigDecimal = typed[BigDecimal]
    val buffer = typed[ByteBuffer]
    val enum = typed[Enumeration#Value]
  }

  def materializer[T : c.WeakTypeTag]: c.Expr[Primitive[T]] = {
    val tpe = weakTypeOf[T].typeSymbol

    val tree = tpe match {
      case Symbols.boolSymbol => booleanPrimitive
      case Symbols.byteSymbol => bytePrimitive
      case Symbols.shortSymbol => shortPrimitive
      case Symbols.intSymbol => intPrimitive
      case Symbols.longSymbol => longPrimitive
      case Symbols.doubleSymbol => doublePrimitive
      case Symbols.floatSymbol => floatPrimitive
      case Symbols.uuidSymbol => uuidPrimitive
      case Symbols.stringSymbol => stringPrimitive
      case Symbols.dateSymbol => datePrimitive
      case Symbols.dateTimeSymbol => dateTimePrimitive
      case Symbols.localDateSymbol => localDatePrimitive
      case Symbols.jodaLocalDateSymbol => localJodaDatePrimitive
      case Symbols.inetSymbol => inetPrimitive
      case Symbols.bigInt => bigIntPrimitive
      case Symbols.bigDecimal => bigDecimalPrimitive
      case Symbols.buffer => bufferPrimitive
      case Symbols.enum => enumPrimitive[T]()
      case Symbols.listSymbol => listPrimitive[T]()
      case Symbols.setSymbol => setPrimitive[T]()
      case Symbols.mapSymbol => mapPrimitive[T]()
      case _ => c.abort(c.enclosingPosition, s"Cannot find primitive implementation for $tpe")
    }

    c.Expr[Primitive[T]](tree)
  }

  def primitive(nm: String): Tree = q"new $prefix.Primitives.${TypeName(nm)}"

  val booleanPrimitive: Tree = primitive("BooleanIsPrimitive")

  val stringPrimitive: Tree = primitive("StringPrimitive")

  val intPrimitive: Tree = primitive("IntPrimitive")

  val shortPrimitive: Tree = primitive("SmallIntPrimitive")

  val bytePrimitive: Tree = primitive("TinyIntPrimitive")

  val doublePrimitive: Tree = primitive("DoublePrimitive")

  val longPrimitive: Tree = primitive("LongPrimitive")

  val floatPrimitive: Tree = primitive("FloatPrimitive")

  val uuidPrimitive: Tree = primitive("UUIDPrimitive")

  val datePrimitive: Tree = primitive("DateIsPrimitive")

  val localDatePrimitive: Tree = primitive("LocalDateIsPrimitive")

  val dateTimePrimitive: Tree = primitive("DateTimeIsPrimitive")

  val localJodaDatePrimitive: Tree = primitive("JodaLocalDateIsPrimitive")

  val bigDecimalPrimitive: Tree = primitive("BigDecimalIsPrimitive")

  val inetPrimitive: Tree = primitive("InetAddressPrimitive")

  val bigIntPrimitive: Tree = primitive("BigIntPrimitive")

  val bufferPrimitive: Tree = primitive("BlobIsPrimitive")

  def listPrimitive[T : WeakTypeTag](): Tree = {
    val tpe = weakTypeOf[T]

    val innerTpe = tpe.typeArgs.headOption

    innerTpe match {
      case Some(inner) => {
        q"""$prefix.Primitives.list[$inner]"""
      }
      case None => c.abort(c.enclosingPosition, "Expected inner type to be defined")
    }
  }

  def mapPrimitive[T : WeakTypeTag](): Tree = {
    val tpe = weakTypeOf[T]

    val keyTpe = tpe.typeArgs.headOption
    val valueType = tpe.typeArgs.drop(1).headOption

    (keyTpe, valueType) match {
      case (Some(key), Some(value)) => {
        q"""$prefix.Primitives.map[$key, $value]"""
      }
      case _ => c.abort(c.enclosingPosition, "Expected inner type to be defined")
    }
  }

  def setPrimitive[T : WeakTypeTag](): Tree = {
    val tpe = weakTypeOf[T]

    val innerTpe = tpe.typeArgs.headOption

    innerTpe match {
      case Some(inner) => {
        q"""$prefix.Primitives.set[$inner]"""
      }
      case None => c.abort(c.enclosingPosition, "Expected inner type to be defined")
    }
  }

  def enumPrimitive[T]()(implicit tag: WeakTypeTag[T]): Tree = {
    val tpe = tag.tpe
    val comp = c.parse(s"${tag.tpe.toString.replace("#Value", "")}")

    q""" new $prefix.Primitive[$tpe] {
      val strP = implicitly[$prefix.Primitive[$strType]]

      override type PrimitiveType = java.lang.String

      override def cassandraType: $strType = strP.cassandraType

      override def fromRow(name: $strType, row: $rowType): scala.util.Try[$tpe] = {
        nullCheck(name, row) {
          r => $comp.values.find(_.toString == r.getString(name)) match {
            case Some(value) => value
            case _ => throw new Exception("Value not found in enumeration") with scala.util.control.NoStackTrace
          }
        }
      }

      override def asCql(value: $tpe): $strType = {
        strP.asCql(value.toString)
      }

      override def fromString(value: $strType): $tpe = {
        $comp.values.find(value == _.toString).getOrElse(scala.None.orNull)
      }

      override def clz: Class[$strType] = classOf[$strType]
    }"""
  }
}
