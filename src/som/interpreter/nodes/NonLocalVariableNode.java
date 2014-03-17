package som.interpreter.nodes;

import static som.interpreter.TruffleCompiler.transferToInterpreter;
import som.vm.Universe;
import som.vmobjects.SClass;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;


public abstract class NonLocalVariableNode extends ContextualNode {

  protected final FrameSlot slot;

  private NonLocalVariableNode(final int contextLevel, final FrameSlot slot,
      final FrameSlot localSelf) {
    super(contextLevel, localSelf);
    this.slot = slot;
  }

  public static class NonLocalVariableReadNode extends NonLocalVariableNode {
    public NonLocalVariableReadNode(final int contextLevel,
        final FrameSlot slot, final FrameSlot localSelf) {
      super(contextLevel, slot, localSelf);
    }

    public NonLocalVariableReadNode(final NonLocalVariableReadNode node) {
      this(node.contextLevel, node.slot, node.localSelf);
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      if (isUninitialized()) {
        return Universe.current().nilObject;
      }
      return FrameUtil.getObjectSafe(determineContext(frame), slot);
    }

    protected final boolean isUninitialized() {
      return slot.getKind() == FrameSlotKind.Illegal;
    }

    @Override
    public final void executeVoid(final VirtualFrame frame) { /* NOOP, side effect free */ }
  }

  public static class NonLocalSuperReadNode
                       extends NonLocalVariableReadNode implements ISuperReadNode {
    private final SClass superClass;

    public NonLocalSuperReadNode(final int contextLevel, final FrameSlot slot,
        final FrameSlot localSelf, final SClass superClass) {
      super(contextLevel, slot, localSelf);
      this.superClass = superClass;
    }

    public NonLocalSuperReadNode(final NonLocalSuperReadNode node) {
      this(node.contextLevel, node.slot, node.localSelf, node.superClass);
    }

    @Override
    public final SClass getSuperClass() {
      return superClass;
    }
  }

  @NodeChild(value = "exp", type = ExpressionNode.class)
  public abstract static class NonLocalVariableWriteNode extends NonLocalVariableNode {

    public NonLocalVariableWriteNode(final int contextLevel,
        final FrameSlot slot, final FrameSlot localSelf) {
      super(contextLevel, slot, localSelf);
    }

    public NonLocalVariableWriteNode(final NonLocalVariableWriteNode node) {
      this(node.contextLevel, node.slot, node.localSelf);
    }

    @Specialization
    public Object writeGeneric(final VirtualFrame frame, final Object expValue) {
      ensureObjectKind();
      determineContext(frame).setObject(slot, expValue);
      return expValue;
    }

    protected final void ensureObjectKind() {
      if (slot.getKind() != FrameSlotKind.Object) {
        transferToInterpreter("LocalVar.writeObjectToUninit");
        slot.setKind(FrameSlotKind.Object);
      }
    }
  }
}
