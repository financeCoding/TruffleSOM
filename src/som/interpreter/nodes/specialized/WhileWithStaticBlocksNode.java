package som.interpreter.nodes.specialized;

import som.interpreter.SArguments;
import som.interpreter.nodes.literals.BlockNode.BlockNodeWithContext;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vm.Universe;
import som.vmobjects.SBlock;
import som.vmobjects.SObject;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.LoopCountReceiver;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.PackedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.CallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;


public abstract class WhileWithStaticBlocksNode extends BinaryExpressionNode {
  @Child protected BlockNodeWithContext receiver;
  @Child protected BlockNodeWithContext argument;
  @Child protected CallNode  conditionValueSend;
  @Child protected CallNode  bodyValueSend;

  protected final SObject predicateBool;
  private final Universe universe;

  private WhileWithStaticBlocksNode(final BlockNodeWithContext receiver,
      final BlockNodeWithContext argument, final SBlock rcvr, final SBlock arg,
      final SObject predicateBool, final Universe universe) {
    this.receiver = adoptChild(receiver);
    this.argument = adoptChild(argument);

    CallTarget callTargetCondition = rcvr.getMethod().getCallTarget();
    conditionValueSend = adoptChild(Truffle.getRuntime().createCallNode(
        callTargetCondition));

    CallTarget callTargetBody = arg.getMethod().getCallTarget();
    bodyValueSend = adoptChild(Truffle.getRuntime().createCallNode(
        callTargetBody));

    this.predicateBool = predicateBool;
    this.universe      = universe;
  }

  @Override
  public final Object executeGeneric(final VirtualFrame frame) {
    SBlock rcvr = receiver.executeSBlock(frame);
    SBlock arg  = argument.executeSBlock(frame);
    return executeEvaluated(frame, rcvr, arg);
  }

  @Override
  public final void executeVoid(final VirtualFrame frame) {
    SBlock rcvr = receiver.executeSBlock(frame);
    SBlock arg  = argument.executeSBlock(frame);
    executeEvaluatedVoid(frame, rcvr, arg);
  }

  @Override
  public final Object executeEvaluated(final VirtualFrame frame,
      final Object rcvr, final Object arg) {
    return doWhileConditionally(frame.pack(), (SBlock) rcvr, (SBlock) arg);
  }

  @Override
  public final void executeEvaluatedVoid(final VirtualFrame frame,
      final Object rcvr, final Object arg) {
    doWhileConditionally(frame.pack(), (SBlock) rcvr, (SBlock) arg);
  }

  protected final SObject doWhileConditionally(final PackedFrame frame,
      final SBlock loopCondition, final SBlock loopBody) {
    int iterationCount = 0;
    SArguments conditionArgs = new SArguments(loopCondition, new Object[0]);
    Object loopConditionResult = conditionValueSend.call(frame, conditionArgs);


    SArguments bodyArgs = new SArguments(loopBody, new Object[0]);
    try {
      // TODO: this is a simplification, we don't cover the case receiver isn't a boolean
      while (loopConditionResult == predicateBool) {
        bodyValueSend.call(frame, bodyArgs);
        loopConditionResult = conditionValueSend.call(frame, conditionArgs);

        if (CompilerDirectives.inInterpreter()) {
          iterationCount++;
        }
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(iterationCount);
      }
    }

    return universe.nilObject;
  }

  protected final void reportLoopCount(final int count) {
    CompilerAsserts.neverPartOfCompilation();
    Node current = getParent();
    while (current != null && !(current instanceof RootNode)) {
      current = current.getParent();
    }
    if (current != null) {
      RootNode root = (RootNode) current;
      if (root.getCallTarget() instanceof LoopCountReceiver) {
        ((LoopCountReceiver) root.getCallTarget()).reportLoopCount(count);
      }
    }
  }

  public static final class WhileTrueStaticBlocksNode extends WhileWithStaticBlocksNode {
    public WhileTrueStaticBlocksNode(final BlockNodeWithContext receiver,
        final BlockNodeWithContext argument, final SBlock rcvr, final SBlock arg,
        final Universe universe) {
      super(receiver, argument, rcvr, arg, universe.trueObject, universe);
    }
  }

  public static final class WhileFalseStaticBlocksNode extends WhileWithStaticBlocksNode {
    public WhileFalseStaticBlocksNode(final BlockNodeWithContext receiver,
        final BlockNodeWithContext argument, final SBlock rcvr, final SBlock arg,
        final Universe universe) {
      super(receiver, argument, rcvr, arg, universe.falseObject, universe);
    }
  }
}
