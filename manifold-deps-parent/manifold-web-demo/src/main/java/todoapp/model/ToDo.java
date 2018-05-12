package todoapp.model;


import java.util.*;
import java.util.stream.*;

public class ToDo {

  private String title;
  private String id;
  private Status status;

  private ToDo(String title, String id, Status status) {
    this.title = title;
    this.id = id;
    this.status = status;
  }

  public void toggleStatus() {
    this.status = isComplete() ? Status.ACTIVE : Status.COMPLETE;
  }

  public boolean isComplete() {
    return this.status == Status.COMPLETE;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public static ToDo create(String title) {
    return new ToDo(title, UUID.randomUUID().toString(), Status.ACTIVE);
  }

  public enum Status {
    ACTIVE,
    COMPLETE
  }
  
  public static class DAO {

    private static final List<ToDo> DATA = new ArrayList<>();

    public static void add(ToDo ToDo) {
      DATA.add(ToDo);
    }

    public static ToDo find(String id) {
      return DATA.first(t -> t.getId().equals(id));
    }

    public static void update(String id, String title) {
      find(id).setTitle(title);
    }

    public static List<ToDo> ofStatus(String statusString) {
      return (statusString == null || statusString.isEmpty()) ? DATA : ofStatus(Status.valueOf(statusString.toUpperCase()));
    }

    public static List<ToDo> ofStatus(Status status) {
      return DATA.filterToList(t -> t.getStatus().equals(status));
    }

    public static void remove(String id) {
      DATA.remove(find(id));
    }

    public static void removeCompleted() {
      ofStatus(Status.COMPLETE).forEach(t -> remove(t.getId()));
    }

    public static void toggleStatus(String id) {
      find(id).toggleStatus();
    }

    public static void toggleAll(boolean complete) {
      all().forEach(t -> t.setStatus(complete ? Status.COMPLETE : Status.ACTIVE));
    }

    public static List<ToDo> all() {
      return DATA;
    }
  }

}
