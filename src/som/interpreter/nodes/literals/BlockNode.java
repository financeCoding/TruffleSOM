package som.interpreter.nodes.literals;

import static com.oracle.truffle.api.nodes.NodeInfo.Kind.SPECIALIZED;
import som.interpreter.Inliner;
import som.interpreter.Invokable;
import som.interpreter.SArguments;
import som.vm.Universe;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;
import som.vmobjects.SInvokable.SMethod;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo.Kind;

public class BlockNode extends LiteralNode {

  protected final SMethod  blockMethod;
  protected final Universe universe;

  public BlockNode(final SMethod blockMethod, final Universe universe) {
    this.blockMethod  = blockMethod;
    this.universe     = universe;
  }

  @Override
  public SBlock executeSBlock(final VirtualFrame frame) {
    return universe.newBlock(blockMethod, null, null);
  }

  @Override
  public final Object executeGeneric(final VirtualFrame frame) {
    return executeSBlock(frame);
  }

  @Override
  public void replaceWithIndependentCopyForInlining(final Inliner inliner) {
    SMethod forInlining = (SMethod) cloneMethod(inliner);
    replace(new BlockNode(forInlining, universe));
  }

  protected SInvokable cloneMethod(final Inliner inliner) {
    Invokable clonedInvokable = blockMethod.getInvokable().
        cloneWithNewLexicalContext(inliner.getLexicalContext());
    SInvokable forInlining = universe.newMethod(blockMethod.getSignature(),
        clonedInvokable, false);
    return forInlining;
  }

  @Override
  public Kind getKind() {
      return SPECIALIZED;
  }

  public static final class BlockNodeWithContext extends BlockNode {
    private final int       contextLevel;

    public BlockNodeWithContext(final SMethod blockMethod,
        final Universe universe,
        final int contextLevel) {
      super(blockMethod, universe);
      this.contextLevel  = contextLevel;
    }

    public BlockNodeWithContext(final BlockNodeWithContext node) {
      this(node.blockMethod, node.universe, node.contextLevel);
    }

    public Object getOuterSelf(final MaterializedFrame frame) {
      return SArguments.getReceiverFromFrame(frame);
    }

    @Override
    public SBlock executeSBlock(final VirtualFrame frame) {
      return universe.newBlock(blockMethod, frame.materialize(), this);
    }
  }
}
