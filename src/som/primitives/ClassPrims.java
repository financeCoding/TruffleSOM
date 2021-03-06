package som.primitives;

import som.interpreter.nodes.nary.UnaryExpressionNode.UnarySideEffectFreeExpressionNode;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SClass;

import com.oracle.truffle.api.dsl.Specialization;


public class ClassPrims {

  public abstract static class NamePrim extends UnarySideEffectFreeExpressionNode {
    @Specialization
    public final SAbstractObject doSClass(final SClass receiver) {
      return receiver.getName();
    }
  }

  public abstract static class SuperClassPrim extends UnarySideEffectFreeExpressionNode {
    @Specialization
    public final SAbstractObject doSClass(final SClass receiver) {
      return receiver.getSuperClass();
    }
  }

  public abstract static class InstanceInvokablesPrim extends UnarySideEffectFreeExpressionNode {
    @Specialization
    public final Object[] doSClass(final SClass receiver) {
      return receiver.getInstanceInvokables();
    }
  }

  public abstract static class InstanceFieldsPrim extends UnarySideEffectFreeExpressionNode {
    @Specialization
    public final Object[] doSClass(final SClass receiver) {
      return receiver.getInstanceFields();
    }
  }
}
