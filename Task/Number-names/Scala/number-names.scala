package org.rosettacode
package numbernames

import annotation.tailrec
import collection.parallel.ParSeq

/** Spells a number longhand
 *
 *  The implementation goes up to 10<sup>69</sup>-1 and also supports negative and zero inputs.
 *
 *  @example longhand( 1234 )  // results in: "one thousand two hundred thirty-four".
 */
object SpellNumber extends App {

  /** Spells a number longhand
   *
   *  The solution is recursive by the triplets of decimal numbers.
   *  @param v				the numeric value to be converted
   *  @param showAnd		flag the output extra and in output, default off
   *  @param zeroString		the word for 0, default to "zero"
   *  @param showHyphen		shows the hyphen e.g. twenty-four, default is on
   *  @return				the numeric value in words
   */
  def longhand(v: BigInt,
               zeroString: String = "zero",
               showAnd: Boolean = false,
               showHyphen: Boolean = true): String = {

    val onesAndTeens = {
      val a1 = "one two three four five six seven eight nine ten eleven twelve".split(' ').map(_ + " ")
      val a2 = "thir four fif six seven eigh nine".split(' ').map(_ + "teen ")
      (Array[String]("") ++ a1 ++ a2).par
    }

    val tens = (Array[String]("", "") ++ ("twen thir for fif six seven eigh nine".split(' ')).
      map(_ + "ty")).par
    val hundredString = "hundred "
    val condAndString = if (showAnd) "and " else ""
    val condHyphenString = if (showHyphen) "-" else " "
    val powersOfThousands = {
      val p1 = "m b tr quadr quint sext sept oct non dec".split(' ').map(_ + "illion ").par
      val p2 = "un duo tre quattuor quin sex septen octo novem ".split(' ').map(_ + "decillion ").par
      val p3 = "vigint cent".split(' ').map(_ + "illion ") ++ Array("overflow trap").par
      (Array("", "thousand ") ++ p1 ++ p2 ++ p3).par
    }

    // 234 becomes "two hundred [and] thirty-four"
    def tripletGroup(nnn: String, isLSDgroup: Boolean, strE3: String): String = {
      nnn match { // Rare exceptions confirms the rule
        case "000" => ""
        case "100" => onesAndTeens(1) + hundredString + strE3 // Solves the faulty hundred AND thousand problem
        case _ => {
          val eval = (nnn.par.map(_.toString.toInt).reverse zip ParSeq('units, 'tens, 'hundreds)).reverse

          eval.map {
            case (d, 'units) if eval.seq.contains((1, 'tens)) => onesAndTeens(d + 10)
            case (d, 'units) if (isLSDgroup && nnn == "0") => zeroString
            case (d, 'units) => onesAndTeens(d)
            case (d, 'hundreds) if d > 0 => onesAndTeens(d) + hundredString + condAndString
            case (d, 'tens) if d > 1 && eval.seq.contains((0, 'units)) => tens(d)
            case (d, 'tens) if d > 1 => tens(d) + condHyphenString //'
            case _ => ""
          }.mkString + strE3
        }
      }
    } // def tripletGroup(…

    def compose(n: BigInt): String = {
      // "1234" becomes List((1,"thousand"), (234, ""))
      val decGroups = n.toString.reverse.grouped(3).map(_.reverse).toSeq.par // Group into powers of thousands
        .zip(powersOfThousands) //						// Name the powers of Thousands
        .reverse //										// Put it back to most-significant first

      if (decGroups.size < 24) // Detect overflow trap
      { // Send per group sections to composeScale
        @tailrec
        def iter(elems: Seq[(String, String)], acc: String): String = {
          elems match {
            case (group, powers) :: tail => {
              iter(tail, acc + tripletGroup(group, tail == Nil, powers))
            }
            case _ => acc
          }
        }
        iter(decGroups.toList, "").mkString.trim
      } else "###.overflow.###"
    } // def compose(…

    // Here starts def longhand(…
    if (v < 0) "minus " + compose(-v) else compose(v)
  } // End def longhand(… @ 91 lines

  // Main entry A little test...
  { // Anonymous ordered list as test set
    def testVal1 = BigInt("1" * 69)
    def testVal9 = BigInt(10).pow(69) - 1

    @tailrec // Series generator of 9, 98, 987, 9876 …
    def inner(counter: Int, elem: BigInt, testList: ParSeq[BigInt]): ParSeq[BigInt] = {
      if (counter < 20)
        inner(counter + 1, elem * 10 + (9 - (counter % 10)), testList ++ ParSeq(elem))
      else testList.par
    }
    inner(0, 0L, // Test values
      ParSeq(-Long.MaxValue, -1000000000, 12, 13, 19, 20, 21, 112, 1001, 1012, 1013,
        Long.MaxValue - 1, Long.MaxValue - 13, testVal1, testVal9)) ++
      (for (z <- 0 to 69) yield BigInt(10).pow(z)) // powers of ten
  }.seq.sorted.
    foreach(num => println(f"""$num%+,80d -> ${longhand(v = num, showAnd = true)}"""))
}  // object SpellNumber
