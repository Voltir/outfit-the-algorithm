import upickle._

package object forceit {
  //implicit def FORCE_COMPILE_DAMNIT[T]: Reader[T] = { assert(false,"FORCE COMPILE DAMNIT!") ; ??? }; implicit def STAB_WRITER[T]: Writer[T] = { assert(false,"FORCE COMPILE DAMNIT!") ; ??? }
}