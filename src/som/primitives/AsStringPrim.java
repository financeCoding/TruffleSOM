package som.primitives;

import java.math.BigInteger;

import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.Specialization;


public abstract class AsStringPrim extends UnaryExpressionNode {

  @Specialization
  public String doSSymbol(final SSymbol receiver) {
    return receiver.getString();
  }

  @SlowPath
  @Specialization
  public String doInteger(final int receiver) {
    return Integer.toString(receiver);
  }

  @SlowPath
  @Specialization
  public String doDouble(final double receiver) {
    return Double.toString(receiver);
  }

  @SlowPath
  @Specialization
  public String doBigInteger(final BigInteger receiver) {
    return receiver.toString();
  }
}
