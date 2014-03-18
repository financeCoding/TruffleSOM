package som.compiler;

import static som.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;
import som.interpreter.nodes.ArgumentReadNode;
import som.interpreter.nodes.ArgumentReadNode.NonLocalArgumentReadNode;
import som.interpreter.nodes.ArgumentReadNode.NonLocalSelfArgumentReadNode;
import som.interpreter.nodes.ArgumentReadNode.SelfArgumentReadNode;
import som.interpreter.nodes.ContextualNode;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.UninitializedVariableNode.UninitializedSuperReadNode;
import som.interpreter.nodes.UninitializedVariableNode.UninitializedVariableReadNode;
import som.interpreter.nodes.UninitializedVariableNode.UninitializedVariableWriteNode;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlot;

public abstract class Variable {
  public final String name;
  public final FrameSlot slot;

  @CompilationFinal protected boolean isRead;
  @CompilationFinal protected boolean isReadOutOfContext;

  Variable(final String name, final FrameSlot slot) {
    this.name      = name;
    this.slot      = slot;
    this.isRead    = false;
    this.isReadOutOfContext = false;
  }

  @Override
  public String toString() {
    return getClass().getName() + "(" + name + ")";
  }

  public final FrameSlot getSlot() {
    return slot;
  }

  public final Object getSlotIdentifier() {
    return slot.getIdentifier();
  }

  public abstract Variable cloneForInlining(final FrameSlot inlinedSlot);

  public boolean isAccessed() {
    return isRead;
  }

  public boolean isAccessedOutOfContext() {
    return isReadOutOfContext;
  }

  public abstract ExpressionNode getReadNode(final int contextLevel);

  public final UninitializedSuperReadNode getSuperReadNode(final int contextLevel,
      final SSymbol holderClass, final boolean classSide) {
    isRead = true;
    if (contextLevel > 0) {
      isReadOutOfContext = true;
    }
    return new UninitializedSuperReadNode(this, contextLevel,
        holderClass, classSide);
  }

  public static final class Argument extends Variable {
    public final int index;

    Argument(final String name, final FrameSlot slot, final int index) {
      super(name, slot);
      this.index = index;
    }

    @Override
    public Variable cloneForInlining(final FrameSlot inlinedSlot) {
      Argument arg = new Argument(name, inlinedSlot, index);
      arg.isRead = isRead;
      arg.isReadOutOfContext = isReadOutOfContext;
      return arg;
    }

    public boolean isSelf() {
      return "self".equals(name) || "$blockSelf".equals(name);
    }

    @Override
    public ExpressionNode getReadNode(final int contextLevel) {
      transferToInterpreterAndInvalidate("Variable.getReadNode");
      isRead = true;
      if (contextLevel > 0) {
        isReadOutOfContext = true;
      }
      if (isSelf()) {
        if (contextLevel > 0) {
          return new NonLocalSelfArgumentReadNode(contextLevel);
        }
        return new SelfArgumentReadNode();
      }
      if (contextLevel > 0) {
        return new NonLocalArgumentReadNode(contextLevel, index);
      }
      return new ArgumentReadNode(index);
    }
  }

  public static final class Local extends Variable {
    @CompilationFinal private boolean isWritten;
    @CompilationFinal private boolean isWrittenOutOfContext;

    Local(final String name, final FrameSlot slot) {
      super(name, slot);
      this.isWritten = false;
      this.isWrittenOutOfContext = false;
    }

    @Override
    public Variable cloneForInlining(final FrameSlot inlinedSlot) {
      Local local = new Local(name, inlinedSlot);
      local.isRead = isRead;
      local.isReadOutOfContext = isReadOutOfContext;
      local.isWritten = isWritten;
      local.isWrittenOutOfContext = isWrittenOutOfContext;
      return local;
    }

    @Override
    public boolean isAccessed() {
      return super.isAccessed() || isWritten;
    }

    @Override
    public boolean isAccessedOutOfContext() {
      return super.isAccessedOutOfContext() || isWrittenOutOfContext;
    }

    @Override
    public ContextualNode getReadNode(final int contextLevel) {
      transferToInterpreterAndInvalidate("Variable.getReadNode");
      isRead = true;
      if (contextLevel > 0) {
        isReadOutOfContext = true;
      }
      return new UninitializedVariableReadNode(this, contextLevel);
    }

    public ExpressionNode getWriteNode(final int contextLevel,
        final ExpressionNode valueExpr) {
      transferToInterpreterAndInvalidate("Variable.getWriteNode");
      isWritten = true;
      if (contextLevel > 0) {
        isWrittenOutOfContext = true;
      }
      return new UninitializedVariableWriteNode(this, contextLevel, valueExpr);
    }
  }
}
