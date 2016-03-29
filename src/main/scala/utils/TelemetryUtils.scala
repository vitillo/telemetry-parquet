package utils

import org.json4s.JsonAST.{JInt, JNothing, JObject, JValue, JString}

object TelemetryUtils{
  // Find the largest numeric bucket that contains a value greater than zero.
  def enumHistogramToCount(h: JValue): Option[Long] = {
    (h \ "values") match {
      case JNothing => None
      case JObject(x) => {
        var topBucket = -1
        for {
          (k, v) <- x
          b <- toInt(k) if b > topBucket && gtZero(v)
        } topBucket = b

        if (topBucket >= 0) {
          Some(topBucket)
        } else {
          None
        }
      }
      case _ => {
        None
      }
    }
  }

  // Given histogram h, return true if it has a value in the "true" bucket,
  // or false if it has a value in the "false" bucket, or None otherwise.
  def booleanHistogramToBoolean(h: JValue): Option[Boolean] = {
    (gtZero(h \ "values" \ "1"), gtZero(h \ "values" \ "0")) match {
      case (true, _) => Some(true)
      case (_, true) => Some(false)
      case _ => None
    }
  }

  // Count the number of keys inside a JSON Object
  def countKeys(o: JValue): Option[Long] = {
    o match {
      case JObject(x) => Some(x.length)
      case _ => {
        None
      }
    }
  }

  // Check if a json value contains a number greater than zero.
  def gtZero(v: JValue): Boolean = {
    v match {
      case x: JInt => x.num.toInt > 0
      case _ => false
    }
  }

  def toInt(s: String): Option[Int] = {
    try {
      Some(s.toInt)
    } catch {
      case e: Exception => None
    }
  }

  def compareFlashVersions(a: Option[String], b: Option[String]): Option[Int] = {
    (a, b) match {
      case (Some(a), None) => Some(1)
      case (None, Some(b)) => Some(-1)
      case (Some(a), Some(b)) => {
        // Shortcut if they're the same string
        if (a == b) return Some(0)

        // Otherwise split them up and compare components numerically.
        val ac = a.split('.')
        val bc = b.split('.')
        var l = ac.length
        if (bc.length < l) l = bc.length

        var abad = true
        var bbad = true
        var aci: Array[Int] = null
        var bci: Array[Int] = null
        try {
          aci = ac.map(n => n.toInt)
          abad = false
        } catch {
          case _: NumberFormatException => abad = true
        }
        try {
          bci = bc.map(n => n.toInt)
          bbad = false
        } catch {
          case _: NumberFormatException => bbad = true
        }

        // Both bad... can't compare.
        if (abad && bbad) return None

        // Good > Bad
        if (abad) return Some(1)
        if (bbad) return Some(0)

        for (versionPiece <- aci.zipAll(bci, 0, 0)) {
          if (versionPiece._1 < versionPiece._2) return Some(-1)
          if (versionPiece._1 > versionPiece._2) return Some(1)
        }

        // They're the same.
        return Some(0)
      }
      case _ => None
    }
  }

  def maxFlashVersion1(a: Option[String], b: Option[String]): String = {
    val c = compareFlashVersions(a, b).getOrElse(1)
    if (c < 0)
      b.getOrElse("")
    else
      a.getOrElse(b.getOrElse(""))
  }

  def maxFlashVersion2(a: String, b: String): String = {
    val c = compareFlashVersions(Some(a), Some(b)).getOrElse(1)
    if (c < 0)
      b
    else
      a
  }

  def maxFlashVersion3(a: Option[String], b: Option[String]): Option[String] = {
    val c = compareFlashVersions(a, b).getOrElse(1)
    if (c < 0)
      b
    else
      a
  }

  def maxFlashVersion4(a: String, b: String): Option[String] = {
    val c = compareFlashVersions(Some(a), Some(b)).getOrElse(1)
    if (c < 0)
      Some(b)
    else
      Some(a)
  }

  def getFlashVersion(addons: JValue): Option[String] = {
    val v = ((addons \ "activePlugins") match {
      case JObject(x) => x
      case _ => return None
    }).filter((p) => {
      p match {
        case (name: String, attrs: JValue) => {
          (attrs \ "name") match {
            case JString(a) => (a == "Shockwave Flash")
            case _ => false
          }
        }
        case _ => false
      }
    }).flatMap((p) => {
      p match {
        case (name: String, attrs: JValue) => {
          (attrs \ "version") match {
            case JString(x) => Some(x)
            case _ => None
          }
        }
        case _ => None
      }
    }).reduceLeft(maxFlashVersion2(_, _))

    Some(v)
//    Some("Hey")
  }
}
