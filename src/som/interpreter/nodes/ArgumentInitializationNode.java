package som.interpreter.nodes;

import som.interpreter.nodes.NonLocalVariableNode.NonLocalVariableWriteNode;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo.Kind;

/**
 * Initializes the frame slots for self as well as the arguments.
 */
public final class ArgumentInitializationNode extends ExpressionNode {
  @Children private final NonLocalVariableWriteNode[] argumentInits;
  @Child    private       ExpressionNode           methodBody;

  public ArgumentInitializationNode(final NonLocalVariableWriteNode[] argumentInits,
      final ExpressionNode methodBody) {
    this.argumentInits = adoptChildren(argumentInits);
    this.methodBody    = adoptChild(methodBody);
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    executeAllArguments(frame);
    return methodBody.executeGeneric(frame);
  }

  @Override
  public void executeVoid(final VirtualFrame frame) {
    executeAllArguments(frame);
    methodBody.executeVoid(frame);
  }

  @ExplodeLoop
  private void executeAllArguments(final VirtualFrame frame) {
    for (int i = 0; i < argumentInits.length; i++) {
      argumentInits[i].executeVoid(frame);
    }
  }

  @Override
  public ExpressionNode getFirstMethodBodyNode() {
    return methodBody;
  }

  @Override
  public Kind getKind() {
      return Kind.GENERIC;
  }
}
