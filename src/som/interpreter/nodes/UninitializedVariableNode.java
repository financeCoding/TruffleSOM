package som.interpreter.nodes;

import static som.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;
import som.compiler.Variable;
import som.compiler.Variable.Argument;
import som.compiler.Variable.Local;
import som.interpreter.Inliner;
import som.interpreter.nodes.ArgumentReadNode.LocalSuperReadNode;
import som.interpreter.nodes.ArgumentReadNode.NonLocalSuperReadNode;
import som.interpreter.nodes.LocalVariableNode.LocalVariableReadNode;
import som.interpreter.nodes.LocalVariableNode.LocalVariableWriteNode;
import som.interpreter.nodes.LocalVariableNodeFactory.LocalVariableReadNodeFactory;
import som.interpreter.nodes.LocalVariableNodeFactory.LocalVariableWriteNodeFactory;
import som.interpreter.nodes.NonLocalVariableNode.NonLocalVariableReadNode;
import som.interpreter.nodes.NonLocalVariableNode.NonLocalVariableWriteNode;
import som.interpreter.nodes.NonLocalVariableNodeFactory.NonLocalVariableReadNodeFactory;
import som.interpreter.nodes.NonLocalVariableNodeFactory.NonLocalVariableWriteNodeFactory;
import som.vm.Universe;
import som.vmobjects.SClass;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;


public abstract class UninitializedVariableNode extends ContextualNode {
  protected final Variable variable;

  public UninitializedVariableNode(final Variable variable,
      final int contextLevel) {
    super(contextLevel);
    this.variable = variable;
  }

  public static final class UninitializedVariableReadNode extends UninitializedVariableNode {
    public UninitializedVariableReadNode(final Variable variable,
        final int contextLevel) {
      super(variable, contextLevel);
    }

    public UninitializedVariableReadNode(final UninitializedVariableReadNode node,
        final FrameSlot inlinedVarSlot) {
      this(node.variable.cloneForInlining(inlinedVarSlot), node.contextLevel);
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      transferToInterpreterAndInvalidate("UninitializedVariableReadNode");

      if (contextLevel > 0) {
        NonLocalVariableReadNode node = NonLocalVariableReadNodeFactory.create(
            contextLevel, variable.slot);
        return replace(node).executeGeneric(frame);
      } else {
        LocalVariableReadNode node = LocalVariableReadNodeFactory.create(variable);
        return replace(node).executeGeneric(frame);
      }
    }

    @Override
    public void executeVoid(final VirtualFrame frame) { /* NOOP, side effect free */ }

    @Override
    public void replaceWithIndependentCopyForInlining(final Inliner inliner) {
      FrameSlot varSlot       = inliner.getFrameSlot(this, variable.getSlotIdentifier());
      assert varSlot       != null;
      replace(new UninitializedVariableReadNode(this, varSlot));
    }

    public boolean accessesArgument() {
      return variable instanceof Argument;
    }

    public boolean accessesTemporary() {
      return variable instanceof Local;
    }

    public boolean accessesSelf() {
      if (accessesTemporary()) {
        return false;
      }
      return ((Argument) variable).isSelf();
    }

    public int getArgumentIndex() {
      if (!accessesArgument()) {
        throw new UnsupportedOperationException("This node does not access an argument.");
      }

      return ((Argument) variable).index;
    }
  }

  public static class UninitializedSuperReadNode extends UninitializedVariableNode {
    private final SSymbol holderClass;
    private final boolean classSide;

    public UninitializedSuperReadNode(final Variable variable,
        final int contextLevel,
        final SSymbol holderClass, final boolean classSide) {
      super(variable, contextLevel);
      this.holderClass = holderClass;
      this.classSide   = classSide;
    }

    public UninitializedSuperReadNode(final UninitializedSuperReadNode node,
        final FrameSlot inlinedVarSlot) {
      this(node.variable.cloneForInlining(inlinedVarSlot), node.contextLevel,
          node.holderClass, node.classSide);
    }

    private SClass getLexicalSuperClass() {
      SClass clazz = (SClass) Universe.current().getGlobal(holderClass);
      if (classSide) {
        clazz = clazz.getSOMClass(Universe.current());
      }
      return (SClass) clazz.getSuperClass();
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      transferToInterpreterAndInvalidate("UninitializedSuperReadNode");

      if (accessesOuterContext()) {
        NonLocalSuperReadNode node = new NonLocalSuperReadNode(contextLevel,
            getLexicalSuperClass());
        return replace(node).executeGeneric(frame);
      } else {
        LocalSuperReadNode node = new LocalSuperReadNode(getLexicalSuperClass());
        return replace(node).executeGeneric(frame);
      }
    }

    @Override
    public void executeVoid(final VirtualFrame frame) { /* NOOP, side effect free */ }

    @Override
    public void replaceWithIndependentCopyForInlining(final Inliner inliner) {
      FrameSlot varSlot = inliner.getFrameSlot(this, variable.getSlotIdentifier());
      assert varSlot != null;
      replace(new UninitializedSuperReadNode(this, varSlot));
    }
  }

  public static final class UninitializedVariableWriteNode extends UninitializedVariableNode {
    @Child private ExpressionNode exp;

    public UninitializedVariableWriteNode(final Local variable,
        final int contextLevel, final ExpressionNode exp) {
      super(variable, contextLevel);
      this.exp = adoptChild(exp);
    }

    public UninitializedVariableWriteNode(final UninitializedVariableWriteNode node,
        final FrameSlot inlinedVarSlot) {
      this((Local) node.variable.cloneForInlining(inlinedVarSlot),
          node.contextLevel, node.exp);
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      transferToInterpreterAndInvalidate("UninitializedVariableWriteNode");

      if (accessesOuterContext()) {
        NonLocalVariableWriteNode node = NonLocalVariableWriteNodeFactory.create(
            contextLevel, variable.slot, exp);
        return replace(node).executeGeneric(frame);
      } else {
        LocalVariableWriteNode node = LocalVariableWriteNodeFactory.create((Local) variable, exp);
        return replace(node).executeGeneric(frame);
      }
    }

    @Override
    public void executeVoid(final VirtualFrame frame) {
      transferToInterpreterAndInvalidate("UninitializedVariableWriteNode");

      if (accessesOuterContext()) {
        NonLocalVariableWriteNode node = NonLocalVariableWriteNodeFactory.create(
            contextLevel, variable.slot, exp);
        replace(node).executeVoid(frame);
      } else {
        LocalVariableWriteNode node = LocalVariableWriteNodeFactory.create((Local) variable, exp);
        replace(node).executeVoid(frame);
      }
    }

    @Override
    public void replaceWithIndependentCopyForInlining(final Inliner inliner) {
      FrameSlot varSlot       = inliner.getFrameSlot(this, variable.getSlotIdentifier());
      assert varSlot       != null;
      replace(new UninitializedVariableWriteNode(this, varSlot));
    }
  }
}
