package manifold.ext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.sourceprod.ResourceFileSourceProducer;

/**
 */
public class Model implements ResourceFileSourceProducer.IModel
{
  private static final Map<String, Model> MODELS = new ConcurrentHashMap<>();

  static Model addFile( String fqnExtended, IFile file, ExtSourceProducer sp )
  {
    Model model = MODELS.get( fqnExtended );
    if( model == null )
    {
      MODELS.put( fqnExtended, model = new Model( fqnExtended, new ArrayList<>(), sp ) );
    }
    if( !model.getFiles().contains( file ) )
    {
      model.getFiles().add( file );
    }
    return model;
  }
  static void clear()
  {
    MODELS.clear();
  }

  private final ExtSourceProducer _sp;
  private final String _fqnExtended;
  private List<IFile> _files;

  Model( String extendedFqn, List<IFile> files, ExtSourceProducer sp )
  {
    _fqnExtended = extendedFqn;
    _files = files;
    _sp = sp;
  }

  @Override
  public String getFqn()
  {
    return _fqnExtended;
  }

  @Override
  public List<IFile> getFiles()
  {
    if( _files == null )
    {
      _files = new ArrayList<>();
    }
    return _files;
  }

  ExtSourceProducer getSourceProducer()
  {
    return _sp;
  }

  void report( DiagnosticListener<JavaFileObject> errorHandler )
  {

  }
}
