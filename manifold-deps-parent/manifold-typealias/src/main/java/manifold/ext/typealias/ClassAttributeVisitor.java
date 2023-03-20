package manifold.ext.typealias;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Type;

import java.util.ArrayList;

class ClassAttributeVisitor implements Attribute.Visitor {

  private final ArrayList<Type> values;

  ClassAttributeVisitor(ArrayList<Type> values) {
    this.values = values;
  }

  @Override
  public void visitConstant(Attribute.Constant constant) {
  }

  @Override
  public void visitClass(Attribute.Class aClass) {
    values.add(aClass.getValue());
  }

  @Override
  public void visitCompound(Attribute.Compound compound) {
  }

  @Override
  public void visitArray(Attribute.Array array) {
    array.getValue().forEach(it -> it.accept(this));
  }

  @Override
  public void visitEnum(Attribute.Enum anEnum) {
  }

  @Override
  public void visitError(Attribute.Error error) {
  }
}
