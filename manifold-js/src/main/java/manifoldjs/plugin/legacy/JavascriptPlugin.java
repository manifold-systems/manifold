package manifoldjs.plugin.legacy;

import gw.config.CommonServices;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFile;
import gw.lang.reflect.IType;
import manifold.api.host.RefreshKind;
import manifold.api.host.RefreshRequest;
import gw.lang.reflect.TypeLoaderBase;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.module.IModule;
import gw.util.Pair;
import gw.util.StreamUtil;
import gw.util.concurrent.LockingLazyVar;
import manifoldjs.parser.Parser;
import manifoldjs.parser.TemplateParser;
import manifoldjs.parser.TemplateTokenizer;
import manifoldjs.parser.Tokenizer;
import manifoldjs.parser.tree.ProgramNode;
import manifoldjs.parser.tree.template.JSTNode;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JavascriptPlugin extends TypeLoaderBase
{

  private static final String JS_EXTENSION = ".js";
  private static final String JST_EXTENSION = ".jst";
  private Set<String> _namespaces;


  private final LockingLazyVar<Map<IFile, String>> _jsFileToName = new LockingLazyVar<Map<IFile, String>>()
  {
    @Override
    protected Map<IFile, String> init()
    {
      /*convert list of <IFile, String> to a map of file names to fully qualified names*/
      return findAllFilesByExtension(JS_EXTENSION).stream()
              .collect(Collectors.toMap(Pair::getSecond, p -> fileToFqn(p.getFirst(), JS_EXTENSION), (p1, p2) -> p1));
    }
  };

  private final LockingLazyVar<Map<String, IFile>> _nameToJSFile = new LockingLazyVar<Map<String, IFile>>()
  {
    @Override
    protected Map<String, IFile> init()
    {
      //invert map of file names to fully qualified names
      return _jsFileToName.get().entrySet().stream()
              .collect(Collectors.toMap(e -> e.getValue(), e -> e.getKey(), (e1, e2) -> e1 ));
    }
  };

  private final LockingLazyVar<Map<IFile, String>> _jstFileToName = new LockingLazyVar<Map<IFile, String>>()
  {
    @Override
    protected Map<IFile, String> init()
    {
      /*convert list of <IFile, String> to a map of file names to fully qualified names*/
      return findAllFilesByExtension(JST_EXTENSION).stream()
              .collect(Collectors.toMap(Pair::getSecond, p -> fileToFqn(p.getFirst(), JST_EXTENSION), (p1, p2) -> p1));
    }
  };

  private final LockingLazyVar<Map<String, IFile>> _nameToJSTFile = new LockingLazyVar<Map<String, IFile>>()
  {
    @Override
    protected Map<String, IFile> init()
    {
      //invert map of file names to fully qualified names
      return _jstFileToName.get().entrySet().stream()
              .collect(Collectors.toMap(e -> e.getValue(), e -> e.getKey(), (e1, e2) -> e1 ));
    }
  };

  private String fileToFqn(String fileName, String extension) {
    return fileName.substring( 0, fileName.length() - extension.length() ).replace( '/', '.' );
  }

  public List<Pair<String, IFile>> findAllFilesByExtension( String extension )
  {
    List<Pair<String, IFile>> results = new ArrayList<>();

    for( IDirectory sourceEntry : _module.getSourcePath() )
    {
      if( sourceEntry.exists() )
      {
        String prefix = sourceEntry.getName().equals( IModule.CONFIG_RESOURCE_PREFIX ) ? IModule.CONFIG_RESOURCE_PREFIX : "";
        addAllLocalResourceFilesByExtensionInternal( prefix, sourceEntry, extension, results );
      }
    }
    return results;
  }

  private void addAllLocalResourceFilesByExtensionInternal( String relativePath, IDirectory dir, String extension, List<Pair<String, IFile>> results )
  {
    List<IDirectory> excludedPath = Arrays.asList( _module.getFileRepository().getExcludedPath() );
    if( excludedPath.contains( dir ) )
    {
      return;
    }
    if( !CommonServices.getPlatformHelper().isPathIgnored( relativePath ) )
    {
      for( IFile file : dir.listFiles() )
      {
        if( file.getName().endsWith( extension ) )
        {
          String path = appendResourceNameToPath( relativePath, file.getName() );
          results.add( new Pair<String, IFile>( path, file ) );
        }
      }
      for( IDirectory subdir : dir.listDirs() )
      {
        String path = appendResourceNameToPath( relativePath, subdir.getName() );
        addAllLocalResourceFilesByExtensionInternal( path, subdir, extension, results );
      }
    }
  }

  private static String appendResourceNameToPath( String relativePath, String resourceName )
  {
    String path;
    if( relativePath.length() > 0 )
    {
      path = relativePath + '/' + resourceName;
    }
    else
    {
      path = resourceName;
    }
    return path;
  }


  public JavascriptPlugin( IModule currentModule )
  {
    super( currentModule );
  }

  @Override
  public IType getType( String name )
  {
    IFile iFile = _nameToJSFile.get().get( name );
    try {
      if (iFile == null) {
        //check to see if JST file
        iFile = _nameToJSTFile.get().get(name);
        if (iFile != null) {
          TemplateParser parser = new TemplateParser(new TemplateTokenizer(
                 StreamUtil.getContent(new InputStreamReader(iFile.openInputStream())), true));
          return new JavascriptTemplateType(this, name, iFile, (JSTNode) parser.parse());
        }
      }
      else {
          Parser parser = new Parser(new Tokenizer(StreamUtil.getContent(new InputStreamReader(iFile.openInputStream()))));
          ProgramNode programNode = (ProgramNode) parser.parse();

          if (programNode.errorCount() > 0) {
            programNode.printErrors();
            return null;
          }

          if (parser.isES6Class()) {
            return new JavascriptClassType(this, name, iFile, programNode);
          } else {
            return new JavascriptProgramType(this, name, iFile, programNode);
          }
        }
    } catch (IOException e) {

    }
    return null;
  }


  @Override
  public Set<? extends CharSequence> getAllNamespaces()
  {
    if( _namespaces == null )
    {
      try
      {
        _namespaces = TypeSystem.getNamespacesFromTypeNames( getAllTypeNames(), new HashSet<String>() );
      }
      catch( NullPointerException e )
      {
        //!! hack to get past dependency issue with tests
        return Collections.emptySet();
      }
    }
    return _namespaces;
  }

  @Override
  public List<String> getHandledPrefixes()
  {
    return Collections.emptyList();
  }

  @Override
  public boolean handlesNonPrefixLoads()
  {
    return true;
  }

  @Override
  public boolean handlesFile( IFile file )
  {
    return JS_EXTENSION.substring( 1 ).equals( file.getExtension() );
  }

  public String[] getTypesForFile( IFile file )
  {
    String typeName = _jsFileToName.get().get( file );
    if( typeName != null )
    {
      return new String[]{typeName};
    }
    return NO_TYPES;
  }

  @Override
  public void refreshedNamespace( String namespace, IDirectory iDirectory, RefreshKind kind )
  {
    clear();
    if( _namespaces != null )
    {
      if( kind == RefreshKind.CREATION )
      {
        _namespaces.add( namespace );
      }
      else if( kind == RefreshKind.DELETION )
      {
        _namespaces.remove( namespace );
      }
    }
  }

  @Override
  protected void refreshedImpl()
  {
    clear();
  }

  @Override
  public RefreshKind refreshedFile( IFile file, String[] types, RefreshKind kind )
  {
    clear();
    return kind;
  }

  @Override
  protected void refreshedTypesImpl( RefreshRequest request )
  {
    clear();
  }

  private void clear()
  {
    _nameToJSFile.clear();
    _jsFileToName.clear();
  }

  @Override
  public boolean hasNamespace( String namespace )
  {
    return getAllNamespaces().contains( namespace );
  }

  @Override
  public Set<String> computeTypeNames()
  {
    return _nameToJSFile.get().keySet();
  }
}
