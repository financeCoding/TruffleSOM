package som.primitives;

import som.primitives.MethodPrimsFactory.HolderPrimFactory;
import som.primitives.MethodPrimsFactory.SignaturePrimFactory;


public final class PrimitivePrimitives extends Primitives {
  @Override
  public void installPrimitives() {
    installInstancePrimitive("signature", SignaturePrimFactory.getInstance());
    installInstancePrimitive("holder", HolderPrimFactory.getInstance());
  }
}
