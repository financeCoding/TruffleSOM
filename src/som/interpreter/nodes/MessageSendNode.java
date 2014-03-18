package som.interpreter.nodes;

import som.interpreter.TruffleCompiler;
import som.interpreter.TypesGen;
import som.interpreter.nodes.dispatch.AbstractDispatchNode;
import som.interpreter.nodes.dispatch.GenericDispatchNode;
import som.interpreter.nodes.dispatch.SuperDispatchNode;
import som.interpreter.nodes.dispatch.UninitializedDispatchNode;
import som.interpreter.nodes.literals.BlockNode;
import som.interpreter.nodes.specialized.IfFalseMessageNodeFactory;
import som.interpreter.nodes.specialized.IfTrueIfFalseMessageNodeFactory;
import som.interpreter.nodes.specialized.IfTrueMessageNodeFactory;
import som.interpreter.nodes.specialized.IntToByDoMessageNodeFactory;
import som.interpreter.nodes.specialized.IntToDoMessageNodeFactory;
import som.interpreter.nodes.specialized.WhileWithStaticBlocksNode.WhileFalseStaticBlocksNode;
import som.interpreter.nodes.specialized.WhileWithStaticBlocksNode.WhileTrueStaticBlocksNode;
import som.vm.Universe;
import som.vmobjects.SBlock;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;



public final class MessageSendNode {

  public static AbstractMessageSendNode create(final SSymbol selector,
      final ExpressionNode receiver, final ExpressionNode[] arguments) {
    return new UninitializedMessageSendNode(selector, receiver, arguments);
  }

  @NodeInfo(shortName = "send")
  public abstract static class AbstractMessageSendNode extends ExpressionNode
      implements PreevaluatedExpression {

    @Child    protected       ExpressionNode       receiverNode;
    @Children protected final ExpressionNode[]     argumentNodes;

    protected AbstractMessageSendNode(final ExpressionNode receiver,
      final ExpressionNode[] arguments) {
      this.receiverNode  = adoptChild(receiver);
      this.argumentNodes = adoptChildren(arguments);
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      Object rcvr = receiverNode.executeGeneric(frame);

      Object[] arguments = evaluateArguments(frame);
      return executePreEvaluated(frame, rcvr, arguments);
    }

    @ExplodeLoop
    private Object[] evaluateArguments(final VirtualFrame frame) {
      Object[] arguments = new Object[argumentNodes.length];
      for (int i = 0; i < argumentNodes.length; i++) {
        arguments[i] = argumentNodes[i].executeGeneric(frame);
      }
      return arguments;
    }

    @Override
    public final void executeVoid(final VirtualFrame frame) {
      executeGeneric(frame);
    }
  }

  private static final class UninitializedMessageSendNode
      extends AbstractMessageSendNode {

    private final SSymbol selector;

    protected UninitializedMessageSendNode(final SSymbol selector,
        final ExpressionNode receiver, final ExpressionNode[] arguments) {
      super(receiver, arguments);
      this.selector = selector;
    }

    @Override
    public Object executePreEvaluated(final VirtualFrame frame,
        final Object receiver, final Object[] arguments) {
      return specialize(receiver, arguments).executePreEvaluated(frame, receiver,
          arguments);
    }

    private PreevaluatedExpression specialize(final Object receiver,
        final Object[] arguments) {
      TruffleCompiler.transferToInterpreterAndInvalidate("Specialize Message Node");

      // first option is a super send, super sends are treated specially because
      // the receiver class is lexically determined
      if (receiverNode instanceof ISuperReadNode) {
        GenericMessageSendNode node = new GenericMessageSendNode(selector,
            receiverNode, argumentNodes, SuperDispatchNode.create(selector,
                (ISuperReadNode) receiverNode));
        return replace(node);
      }

      // We treat super sends separately for simplicity, might not be the
      // optimal solution, especially in cases were the knowledge of the
      // receiver class also allows us to do more specific things, but for the
      // moment  we will leave it at this.
      // TODO: revisit, and also do more specific optimizations for super sends.


      // let's organize the specializations by number of arguments
      // perhaps not the best, but one simple way to just get some order into
      // the chaos.

      switch (argumentNodes.length) {
        case  0: return specializeUnary(receiver);
        case  1: return specializeBinary(receiver,  arguments);
        case  2: return specializeTernary(receiver, arguments);
        case  3: return specializeQuaternary(receiver, arguments);
      }

      return makeGenericSend();
    }

    private GenericMessageSendNode makeGenericSend() {
      GenericMessageSendNode send = new GenericMessageSendNode(selector,
          receiverNode, argumentNodes,
          new UninitializedDispatchNode(selector, Universe.current()));
      return replace(send);
    }

    private PreevaluatedExpression specializeUnary(final Object receiver) {
      return makeGenericSend();
    }

    private PreevaluatedExpression specializeBinary(final Object receiver,
        final Object[] arguments) {
      switch (selector.getString()) {
        case "whileTrue:": {
          if (argumentNodes[0] instanceof BlockNode &&
              receiverNode instanceof BlockNode) {
            BlockNode argBlockNode = (BlockNode) argumentNodes[0];
            SBlock    argBlock     = (SBlock)    arguments[0];
            return replace(new WhileTrueStaticBlocksNode(
                (BlockNode) receiverNode, argBlockNode, (SBlock) receiver,
                argBlock, Universe.current()));
          }
          break; // use normal send
        }
        case "whileFalse:":
          if (argumentNodes[0] instanceof BlockNode &&
              receiverNode instanceof BlockNode) {
            BlockNode argBlockNode = (BlockNode) argumentNodes[0];
            SBlock    argBlock     = (SBlock)    arguments[0];
            return replace(new WhileFalseStaticBlocksNode(
                (BlockNode) receiverNode, argBlockNode,
                (SBlock) receiver, argBlock, Universe.current()));
          }
          break; // use normal send
        case "ifTrue:":
          return replace(IfTrueMessageNodeFactory.create(receiver, arguments[0],
              Universe.current(), receiverNode, argumentNodes[0]));
        case "ifFalse:":
          return replace(IfFalseMessageNodeFactory.create(receiver, arguments[0],
              Universe.current(), receiverNode, argumentNodes[0]));
      }

      return makeGenericSend();
    }

    private PreevaluatedExpression specializeTernary(final Object receiver,
        final Object[] arguments) {
      switch (selector.getString()) {
        case "ifTrue:ifFalse:":
          return replace(IfTrueIfFalseMessageNodeFactory.create(receiver,
              arguments[0], arguments[1], Universe.current(), receiverNode,
              argumentNodes[0], argumentNodes[1]));
        case "to:do:":
          if (TypesGen.TYPES.isImplicitInteger(receiver) &&
              (TypesGen.TYPES.isImplicitInteger(arguments[0]) ||
                  TypesGen.TYPES.isImplicitDouble(arguments[0])) &&
              TypesGen.TYPES.isSBlock(arguments[1])) {
            return replace(IntToDoMessageNodeFactory.create(this,
                (SBlock) arguments[1], receiverNode, argumentNodes[0],
                argumentNodes[1]));
          }
          break;
      }
      return makeGenericSend();
    }

    private PreevaluatedExpression specializeQuaternary(final Object receiver,
        final Object[] arguments) {
      switch (selector.getString()) {
        case "to:by:do:":
          return replace(IntToByDoMessageNodeFactory.create(this,
              (SBlock) arguments[2], receiverNode, argumentNodes[0],
              argumentNodes[1], argumentNodes[2]));
      }
      return makeGenericSend();
    }
  }

  public static final class GenericMessageSendNode
      extends AbstractMessageSendNode {

    private final SSymbol selector;

    public static GenericMessageSendNode create(final SSymbol selector,
        final ExpressionNode receiverNode,
        final ExpressionNode[] argumentNodes) {
      return new GenericMessageSendNode(selector,
          receiverNode, argumentNodes,
          new UninitializedDispatchNode(selector, Universe.current()));
    }

    @Child private AbstractDispatchNode dispatchNode;

    private GenericMessageSendNode(final SSymbol selector,
        final ExpressionNode receiver, final ExpressionNode[] arguments,
        final AbstractDispatchNode dispatchNode) {
      super(receiver, arguments);
      this.selector = selector;
      this.dispatchNode  = adoptChild(dispatchNode);
    }

    @Override
    public Object executePreEvaluated(final VirtualFrame frame,
        final Object receiver, final Object[] arguments) {
      return dispatchNode.executeDispatch(frame, receiver, arguments);
    }

    @SlowPath
    public AbstractDispatchNode getDispatchListHead() {
      return dispatchNode;
    }

    @SlowPath
    public void adoptNewDispatchListHead(final AbstractDispatchNode newHead) {
      dispatchNode = adoptChild(newHead);
    }

    @SlowPath
    public void replaceDispatchListHead(
        final GenericDispatchNode replacement) {
      dispatchNode.replace(replacement);
    }

    @Override
    public String toString() {
      return "GMsgSend(" + selector.getString() + ")";
    }
  }
}
