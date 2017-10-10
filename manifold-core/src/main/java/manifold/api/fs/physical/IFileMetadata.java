package manifold.api.fs.physical;

public interface IFileMetadata
{
  String name();

  boolean isDir();

  boolean isFile();

  boolean exists();

  long lastModifiedTime();

  long length();
}
