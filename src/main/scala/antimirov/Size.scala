package antimirov

/**
 * Extended integer representation of sizes.
 *
 * This class is used to represent the lengths of matched strings.
 * Unbounded values are generated by things like the Kleene star
 * operator.
 *
 * Size supports addition and multiplication. Additionally, Size has a
 * total ordering (unbounded values are considered), so comparisons
 * are also supported.
 *
 * Sizes are required to be non-negative, i.e. 0 <= size < ∞.
 */
sealed abstract class Size { lhs =>

  import Size.{Small, Big, Unbounded}

  private def toBigInt: BigInt =
    this match {
      case Unbounded => sys.error("!")
      case Big(n) => n
      case Small(n) => BigInt(n)
    }

  def +(rhs: Size): Size =
    (lhs, rhs) match {
      case (Unbounded, _) | (_, Unbounded) =>
        Unbounded
      case (Small(x), Small(y)) =>
        val z = x + y
        if (z >= 0) Small(z) else Big(BigInt(x) + BigInt(y))
      case _ =>
        Big(lhs.toBigInt + rhs.toBigInt)
    }

  def *(rhs: Size): Size =
    (lhs, rhs) match {
      case (Size.Zero, _) | (_, Size.Zero) => Size.Zero
      case (Unbounded, _) | (_, Unbounded) => Unbounded
      case (Small(x), Small(y)) =>
        val z = x * y
        if ((z / x) == y) Small(z) else Big(BigInt(x) * BigInt(y))
      case _ =>
        Big(lhs.toBigInt * rhs.toBigInt)
    }

  def pow(k: Int): Size = {
    def loop(i: Int, acc: Size, mult: Size): Size = {
      if (i <= 0) Size.One
      else if (i == 1) mult * acc
      else loop(i / 2, if (i % 2 == 1) acc * mult else acc, mult * mult)
    }
    loop(k, Size.One, this)
  }

  override def toString: String =
    this match {
      case Unbounded => "∞"
      case Small(n) => n.toString
      case Big(n) => n.toString
    }

  /**
   * Show approximate as well as actual size as a string.
   *
   * For unbounded or small sizes, this method is identical to
   * toString.
   *
   * For finite sizes a million or larger, this method will return
   * scientific notation to approximate the number followed by the
   * full decimal representation in parenthesis.
   *
   *   Size(0).approxString               // 0
   *   Size(123456).approxString          // 123456
   *   Size(1234567).approxString         // 1.23 x 10^6 (1234567)
   *   Size(1234567).pow(3).approxString  // 1.88 x 10^18 (1881672302290562263)
   *   Size.Unbounded.approxString        // ∞
   */
  def approxString: String = {
    def approx(num: BigInt): String =
      if (num < 1000000) {
        num.toString
      } else {
        var n = num
        var k = 0
        // make sure n won't round up to 1000 once we exit the loop
        while (n >= 9994) {
          n /= 10
          k += 1
        }
        // round the final digit
        n = (n + 5) / 10 // n in [0, 999]
        k += 1 // k in [6, ∞)
        val s = n.toString
        val d = s.charAt(0)
        val ds = s.substring(1, 3)
        s"$d.$ds x 10^${k + 2} ($this)"
      }

    this match {
      case Unbounded => "∞"
      case _ => approx(this.toBigInt)
    }
  }

  def <(rhs: Size): Boolean = compare(rhs) < 0
  def <=(rhs: Size): Boolean = compare(rhs) <= 0
  def >(rhs: Size): Boolean = compare(rhs) > 0
  def >=(rhs: Size): Boolean = compare(rhs) >= 0

  def min(rhs: Size): Size =
    if (lhs <= rhs) lhs else rhs

  def max(rhs: Size): Size =
    if (lhs >= rhs) lhs else rhs

  def compare(rhs: Size): Int =
    lhs match {
      case Unbounded =>
        if (rhs == Unbounded) 0 else 1
      case Big(x) =>
        rhs match {
          case Unbounded => -1
          case Big(y) => x compare y
          case Small(_) => 1
        }
      case Small(x) =>
        rhs match {
          case Small(y) => java.lang.Long.compare(x, y)
          case _ => -1
        }
    }
}

object Size {

  val Zero = Size(0L)
  val One = Size(1L)

  def apply(n: Int): Size = {
    require(n >= 0)
    Small(n.toLong)
  }

  def apply(n: Long): Size = {
    require(n >= 0L)
    Small(n)
  }

  def apply(n: BigInt): Size = {
    require(n.signum >= 0)
    Big(n)
  }

  case class Small private[antimirov] (n: Long) extends Size
  case class Big private[antimirov] (n: BigInt) extends Size
  case object Unbounded extends Size
}
