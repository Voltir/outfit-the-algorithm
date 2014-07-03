package shared

import org.scalajs.spickling._
import shared.models.PatternRegister
import shared.models._

object AlgoPickler extends BasePicklerRegistry {
  PatternRegister

  override def pickle[P](value: Any)(implicit builder: PBuilder[P], registry: PicklerRegistry): P = {
    value match {
      case opt: Option[_] => opt.fold(builder.makeObject("opt"->builder.makeNull())) { o =>
        builder.makeObject("opt"->registry.pickle(o))
      }
      case arr: Array[Pattern.Assignment] => {
        builder.makeObject("aAss" -> builder.makeArray(arr.map(a => AlgoPickler.pickle(a)):_*))
      }
      case arr: Array[Pattern] => {
        builder.makeObject("aPat" -> builder.makeArray(arr.map(a => AlgoPickler.pickle(a)):_*))
      }
      case _ => super.pickle(value)
    }
  }

  override def unpickle[P](pickle: P)(implicit reader: PReader[P], registry: PicklerRegistry): Any = {
    def load[Hack](data: P) = (0 until reader.readArrayLength(data)).map { i =>
      AlgoPickler.unpickle(reader.readArrayElem(data,i)).asInstanceOf[Hack]
    }
    val aAss = reader.readObjectField(pickle,"aAss")
    if(!reader.isUndefined(aAss)) {
      load[Pattern.Assignment](aAss).toArray
    } else {
      val aPat = reader.readObjectField(pickle,"aPat")
      if(!reader.isUndefined(aPat)) {
        load[Pattern](aPat).toArray
      } else {
        val opt = reader.readObjectField(pickle, "opt")
        if (!reader.isUndefined(opt)) {
          if (reader.isNull(opt)) None
          else Option(AlgoPickler.unpickle(opt))
        } else {
          super.unpickle(pickle)
        }
      }
    }
  }


}
