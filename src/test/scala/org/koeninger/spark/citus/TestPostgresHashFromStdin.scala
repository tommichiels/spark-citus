package org.koeninger.spark.citus

import xbird.util.hashes.JenkinsHash
import java.nio.{ ByteBuffer, ByteOrder }

/** make sure the jenkins hash matches postgres output
you can test against the output of

for Int
psql -c 'copy(select s.i, hashint4(s.i) from generate_series(0,10000000) as s(i)) to stdout with CSV;' |  sbt 'test:runMain org.koeninger.spark.citus.TestPostgresHashFromStdin'

for Long use both negative and positive

psql -c 'copy(select s.i, hashint8(s.i) from generate_series( -9223372036854775807::bigint, -9223372036853775808) as s(i)) to stdout with CSV;' |  sbt 'test:runMain org.koeninger.spark.citus.TestPostgresHashFromStdin'

psql -c 'copy(select s.i, hashint8(s.i) from generate_series( 9223372036854675807::bigint, 9223372036854775806) as s(i)) to stdout with CSV;' |  sbt 'test:runMain org.koeninger.spark.citus.TestPostgresHashFromStdin'

I just don't want to depend on psql server for unit tests, or check in MB of test data
 */
object TestPostgresHashFromStdin {
  def main(args: Array[String]): Unit = {
    val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)

    var count = 0

    Iterator.continually(Console.readLine).takeWhile(_ != null).foreach { line =>
      val Array(a, b) = line.split(",")
      val i = a.toLong
      val expected = b.toLong
      val result = JenkinsHash.postgresHashint8(i)
      if (result != expected) {
        println(s"$i expected $expected got $result")
        System.exit(666)
      }
      count += 1
      bb.clear()
    }

    println(s"successfully checked $count ints")
  }
}
