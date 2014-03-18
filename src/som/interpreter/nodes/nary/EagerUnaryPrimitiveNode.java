package som.interpreter.nodes.nary;

import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.utilities.BranchProfile;


public class EagerUnaryPrimitiveNode extends UnaryExpressionNode {

  @Child private ExpressionNode receiver;
  @Child private UnaryExpressionNode primitive;

  private final BranchProfile unsupportedSpecialization;
  private final SSymbol selector;

  public EagerUnaryPrimitiveNode(final SSymbol selector,
      final ExpressionNode receiver, final UnaryExpressionNode primitive) {
    this.receiver = adoptChild(receiver);
    this.primitive = adoptChild(primitive);

    this.unsupportedSpecialization = new BranchProfile();
    this.selector = selector;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object rcvr = receiver.executeGeneric(frame);

    return executeEvaluated(frame, rcvr);
  }

  @Override
  public Object executeEvaluated(final VirtualFrame frame,
      final Object receiver) {
    try {
      return primitive.executeEvaluated(frame, receiver);
    } catch (UnsupportedSpecializationException e) {
      unsupportedSpecialization.enter();
      return makeGenericSend().executePreEvaluated(frame, receiver, new Object[0]);
    }
  }

  @Override
  public void executeEvaluatedVoid(final VirtualFrame frame,
      final Object receiver) {
    try {
      primitive.executeEvaluatedVoid(frame, receiver);
    } catch (UnsupportedSpecializationException e) {
      unsupportedSpecialization.enter();
      makeGenericSend().executePreEvaluated(frame, receiver, new Object[0]);
    }
  }

  private GenericMessageSendNode makeGenericSend() {
    GenericMessageSendNode node = GenericMessageSendNode.create(selector,
        receiver, new ExpressionNode[0]);
    return replace(node);
  }
}
