package som.primitives.arithmetic;

import com.oracle.truffle.api.dsl.Specialization;


public abstract class BitXorPrim extends ArithmeticPrim {
  @Specialization
  public final long doLong(final long receiver, final long right) {
    return receiver ^ right;
  }
}
