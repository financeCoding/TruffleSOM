package som.interpreter.nodes;

import som.interpreter.SArguments;
import som.vmobjects.SClass;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo.Kind;

public final class ArgumentReadNode extends ExpressionNode implements
    PreevaluatedExpression {

  protected final int argumentIndex;

  public ArgumentReadNode(final int argumentIndex) {
    this.argumentIndex = argumentIndex;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    return SArguments.getArgumentsFromFrame(frame)[argumentIndex];
  }

  @Override
  public Object executePreEvaluated(final VirtualFrame frame,
      final Object receiver, final Object[] arguments) {
    return arguments[argumentIndex];
  }

  public static class SelfArgumentReadNode extends ExpressionNode
      implements PreevaluatedExpression {

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      return SArguments.getReceiverFromFrame(frame);
    }

    @Override
    public final Object executePreEvaluated(final VirtualFrame frame,
        final Object receiver, final Object[] arguments) {
      return receiver;
    }

    @Override
    public final void executeVoid(final VirtualFrame frame) { /* NOOP, side effect free */}
  }

  @Override
  public void executeVoid(final VirtualFrame frame) { /* NOOP, side effect free */}

  public static final class NonLocalArgumentReadNode extends ContextualNode implements
      PreevaluatedExpression {

    protected final int argumentIndex;

    public NonLocalArgumentReadNode(final int contextLevel,
        final int argumentIndex) {
      super(contextLevel);
      this.argumentIndex = argumentIndex;
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      return SArguments.getArgumentsFromFrame(determineContext(frame))[argumentIndex];
    }

    @Override
    public Object executePreEvaluated(final VirtualFrame frame,
        final Object receiver, final Object[] arguments) {
      return arguments[argumentIndex];
    }

    @Override
    public Kind getKind() {
      return Kind.GENERIC;
    }

    @Override
    public void executeVoid(final VirtualFrame frame) { /* NOOP, side effect free */}
  }

  public static class NonLocalSelfArgumentReadNode extends ContextualNode
      implements PreevaluatedExpression {

    public NonLocalSelfArgumentReadNode(final int contextLevel) {
      super(contextLevel);
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      return SArguments.getReceiverFromFrame(determineContext(frame));
    }

    @Override
    public final Object executePreEvaluated(final VirtualFrame frame,
        final Object receiver, final Object[] arguments) {
      return receiver;
    }

    @Override
    public final void executeVoid(final VirtualFrame frame) { /* NOOP, side effect free */}
  }

  public static final class NonLocalSuperReadNode extends NonLocalSelfArgumentReadNode
      implements ISuperReadNode {
    private final SClass superClass;

    public NonLocalSuperReadNode(final int contextLevel, final SClass superClass) {
      super(contextLevel);
      this.superClass = superClass;
    }

    @Override
    public SClass getSuperClass() {
      return superClass;
    }
  }

  public static final class LocalSuperReadNode extends SelfArgumentReadNode
    implements ISuperReadNode {
    private final SClass superClass;

    public LocalSuperReadNode(final SClass superClass) {
      this.superClass = superClass;
    }

    @Override
    public SClass getSuperClass() {
      return superClass;
    }
  }
}
