package manifoldjs;

import java.util.Collections;
import java.util.List;
import manifold.api.fs.IFile;
import manifold.api.sourceprod.ResourceFileSourceProducer;

import java.net.MalformedURLException;

/**
 * Created by carson on 5/10/17.
 */
public class JavascriptModel implements ResourceFileSourceProducer.IModel
{
    private String _fqn;
    private IFile _file;
    String _url;

    JavascriptModel( String fqn, IFile file )
    {
        _fqn = fqn;
        _file = file;
        try
        {
            _url = file.toURI().toURL().toString();
        }
        catch( MalformedURLException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public String getFqn()
    {
        return _fqn;
    }

    @Override
    public List<IFile> getFiles()
    {
        return Collections.singletonList( _file );
    }

    public String getUrl()
    {
        return _url;
    }
}

