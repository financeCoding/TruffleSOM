package som.primitives;

import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.primitives.arithmetic.ArithmeticPrim;
import som.vm.Universe;
import som.vmobjects.SClass;

import com.oracle.truffle.api.dsl.Specialization;

public abstract class IntegerPrims {


  public abstract static class RandomPrim extends UnaryExpressionNode {
    @Specialization
    public int doInteger(final int receiver) {
      return (int) (receiver * Math.random());
    }
  }

  public abstract static class FromStringPrim extends ArithmeticPrim {
    private final Universe universe;
    public FromStringPrim() { this.universe = Universe.current(); }

    protected boolean receiverIsIntegerClass(final SClass receiver) {
      return receiver == universe.integerClass;
    }

    @Specialization(guards = "receiverIsIntegerClass")
    public Object doSClass(final SClass receiver, final String argument) {
      long result = Long.parseLong(argument);
      return intOrBigInt(result);
    }
  }
}
