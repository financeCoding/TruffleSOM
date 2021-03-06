package som.interpreter.nodes.specialized;

import som.interpreter.nodes.PreevaluatedExpression;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.utilities.BranchProfile;

public abstract class IfTrueIfFalseMessageNode extends TernaryExpressionNode
    implements PreevaluatedExpression {
  private final BranchProfile ifFalseBranch = new BranchProfile();
  private final BranchProfile ifTrueBranch  = new BranchProfile();

  private final SInvokable trueMethod;
  private final SInvokable falseMethod;

  @Child protected DirectCallNode trueValueSend;
  @Child protected DirectCallNode falseValueSend;

  @Child private IndirectCallNode call;

  public IfTrueIfFalseMessageNode(final Object rcvr, final Object arg1,
      final Object arg2) {
    if (arg1 instanceof SBlock) {
      SBlock trueBlock = (SBlock) arg1;
      trueMethod = trueBlock.getMethod();
      trueValueSend = Truffle.getRuntime().createDirectCallNode(
          trueMethod.getCallTarget());
    } else {
      trueMethod = null;
    }

    if (arg2 instanceof SBlock) {
      SBlock falseBlock = (SBlock) arg2;
      falseMethod = falseBlock.getMethod();
      falseValueSend = Truffle.getRuntime().createDirectCallNode(
          falseMethod.getCallTarget());
    } else {
      falseMethod = null;
    }

    call = Truffle.getRuntime().createIndirectCallNode();
  }

  public IfTrueIfFalseMessageNode(final IfTrueIfFalseMessageNode node) {
    trueMethod = node.trueMethod;
    if (node.trueMethod != null) {
      trueValueSend = Truffle.getRuntime().createDirectCallNode(
          trueMethod.getCallTarget());
    }

    falseMethod = node.falseMethod;
    if (node.falseMethod != null) {
      falseValueSend = Truffle.getRuntime().createDirectCallNode(
          falseMethod.getCallTarget());
    }
    call = Truffle.getRuntime().createIndirectCallNode();
  }

  @Override
  public final Object doPreEvaluated(final VirtualFrame frame,
      final Object[] arguments) {
    return executeEvaluated(frame, arguments[0], arguments[1], arguments[2]);
  }

  protected final boolean hasSameArguments(final Object receiver, final Object firstArg, final Object secondArg) {
    return (trueMethod  == null || ((SBlock) firstArg).getMethod()  == trueMethod)
        && (falseMethod == null || ((SBlock) secondArg).getMethod() == falseMethod);
  }

  @Specialization(order = 1, guards = "hasSameArguments")
  public final Object doIfTrueIfFalseWithInlining(final VirtualFrame frame,
      final boolean receiver, final SBlock trueBlock, final SBlock falseBlock) {
    if (receiver) {
      ifTrueBranch.enter();
      return trueValueSend.call(frame, new Object[] {trueBlock});
    } else {
      ifFalseBranch.enter();
      return falseValueSend.call(frame, new Object[] {falseBlock});
    }
  }

  @Specialization(order = 10)
  public final Object doIfTrueIfFalse(final VirtualFrame frame,
      final boolean receiver, final SBlock trueBlock, final SBlock falseBlock) {
    CompilerAsserts.neverPartOfCompilation("IfTrueIfFalseMessageNode.10");
    if (receiver) {
      ifTrueBranch.enter();
      return trueBlock.getMethod().invoke(frame, call, trueBlock);
    } else {
      ifFalseBranch.enter();
      return falseBlock.getMethod().invoke(frame, call, falseBlock);
    }
  }

  @Specialization(order = 18, guards = "hasSameArguments")
  public final Object doIfTrueIfFalseWithInlining(final VirtualFrame frame,
      final boolean receiver, final Object trueValue, final SBlock falseBlock) {
    if (receiver) {
      ifTrueBranch.enter();
      return trueValue;
    } else {
      ifFalseBranch.enter();
      return falseValueSend.call(frame, new Object[] {falseBlock});
    }
  }

  @Specialization(order = 19, guards = "hasSameArguments")
  public final Object doIfTrueIfFalseWithInlining(final VirtualFrame frame,
      final boolean receiver, final SBlock trueBlock, final Object falseValue) {
    if (receiver) {
      ifTrueBranch.enter();
      return trueValueSend.call(frame, new Object[] {trueBlock});
    } else {
      ifFalseBranch.enter();
      return falseValue;
    }
  }

  @Specialization(order = 20)
  public final Object doIfTrueIfFalse(final VirtualFrame frame,
      final boolean receiver, final Object trueValue, final SBlock falseBlock) {
    if (receiver) {
      ifTrueBranch.enter();
      return trueValue;
    } else {
      ifFalseBranch.enter();
      CompilerAsserts.neverPartOfCompilation("IfTrueIfFalseMessageNode.20");
      return falseBlock.getMethod().invoke(frame, call, falseBlock);
    }
  }

  @Specialization(order = 30)
  public final Object doIfTrueIfFalse(final VirtualFrame frame,
      final boolean receiver, final SBlock trueBlock, final Object falseValue) {
    if (receiver) {
      ifTrueBranch.enter();
      CompilerAsserts.neverPartOfCompilation("IfTrueIfFalseMessageNode.30");
      return trueBlock.getMethod().invoke(frame, call, trueBlock);
    } else {
      ifFalseBranch.enter();
      return falseValue;
    }
  }

  @Specialization(order = 40)
  public final Object doIfTrueIfFalse(final VirtualFrame frame,
      final boolean receiver, final Object trueValue, final Object falseValue) {
    if (receiver) {
      return trueValue;
    } else {
      return falseValue;
    }
  }
}
