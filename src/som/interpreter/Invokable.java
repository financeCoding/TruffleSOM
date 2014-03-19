package som.interpreter;

import som.interpreter.nodes.ExpressionNode;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.utilities.BranchProfile;

public abstract class Invokable extends RootNode {

  @Child protected ExpressionNode  expressionOrSequence;

  private final ExpressionNode  uninitializedBody;
  protected final FrameSlot frameOnStackMarker;
  private final BranchProfile nonLocalReturnHandler;

  public Invokable(final SourceSection sourceSection,
      final FrameDescriptor frameDescriptor,
      final ExpressionNode expressionOrSequence,
      final FrameSlot frameOnStackMarker) {
    super(sourceSection, frameDescriptor);
    this.uninitializedBody    = NodeUtil.cloneNode(expressionOrSequence);
    this.expressionOrSequence = adoptChild(expressionOrSequence);
    this.frameOnStackMarker   = frameOnStackMarker;
    nonLocalReturnHandler = new BranchProfile();
  }

  public ExpressionNode getUninitializedBody() {
    return uninitializedBody;
  }

  @Override
  public final Object execute(final VirtualFrame frame) {
    FrameOnStackMarker marker = new FrameOnStackMarker();
    frameOnStackMarker.setKind(FrameSlotKind.Object);
    frame.setObject(frameOnStackMarker, marker);

    Object result;
    try {
      return expressionOrSequence.executeGeneric(frame);
    } catch (ReturnException e) {
      nonLocalReturnHandler.enter();
      if (!e.reachedTarget(marker)) {
        throw e;
      } else {
        result = e.result();
      }
    } finally {
      marker.frameNoLongerOnStack();
    }
    return result;


  }

  public abstract Invokable cloneWithNewLexicalContext(final LexicalContext outerContext);

  @Override
  public boolean isSplittable() {
    return true;
  }

  public final RootCallTarget createCallTarget() {
    return Truffle.getRuntime().createCallTarget(this);
  }

  public abstract boolean isAlwaysToBeInlined();
}
