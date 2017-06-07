package manifoldjs.plugin.legacy;

import manifold.api.fs.IFile;
import gw.lang.reflect.IMetaType;
import gw.lang.reflect.IType;
import gw.lang.reflect.ITypeInfo;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.gs.IGenericTypeVariable;
import manifoldjs.parser.tree.ClassNode;
import manifoldjs.parser.tree.ProgramNode;

public class JavascriptClassType extends JavascriptTypeBase
{
  private final JavascriptClassTypeInfo _typeinfo;
  private ProgramNode _programNode;
  private IType _superType;


  public JavascriptClassType(JavascriptPlugin typeloader, String name, IFile jsFile, ProgramNode programNode)
  {
    super( typeloader, name, jsFile );
    _typeinfo = new JavascriptClassTypeInfo( this, programNode );
    _programNode = programNode;

    String packageName = _programNode.getPackageFromClassName(_programNode.getFirstChild(ClassNode.class)
            .getSuperClass());
    if (packageName != null) _superType = TypeSystem.getByFullName(packageName);
  }

  @Override
  public ITypeInfo getTypeInfo()
  {
    return _typeinfo;
  }


}
