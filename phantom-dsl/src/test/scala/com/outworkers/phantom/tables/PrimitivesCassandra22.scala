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
package com.outworkers.phantom.tables

import com.outworkers.phantom.connectors.RootConnector
import com.outworkers.phantom.builder.query.InsertQuery
import com.outworkers.phantom.dsl._
import org.joda.time.LocalDate

case class PrimitiveCassandra22(
  pkey: String,
  short: Short,
  byte: Byte,
  date: LocalDate
)

sealed class PrimitivesCassandra22 extends CassandraTable[ConcretePrimitivesCassandra22, PrimitiveCassandra22] {

  object pkey extends StringColumn(this) with PartitionKey[String]

  object short extends SmallIntColumn(this)

  object byte extends TinyIntColumn(this)

  object date extends LocalDateColumn(this)

  override def fromRow(r: Row): PrimitiveCassandra22 = {
    PrimitiveCassandra22(
      pkey = pkey(r),
      short = short(r),
      byte = byte(r),
      date = date(r)
    )
  }
}

abstract class ConcretePrimitivesCassandra22 extends PrimitivesCassandra22 with RootConnector {

  override val tableName = "PrimitivesCassandra22"

  def store(row: PrimitiveCassandra22): InsertQuery.Default[ConcretePrimitivesCassandra22, PrimitiveCassandra22] = {
    insert
      .value(_.pkey, row.pkey)
      .value(_.short, row.short)
      .value(_.byte, row.byte)
      .value(_.date, row.date)
  }
}
