package todoapp;

import manifold.templates.runtime.ILayout;
import spark.Request;
import todoapp.model.ToDo;
import todoapp.model.ToDo.Status;
import todoapp.view.layouts.Main;
import todoapp.view.todo.Display;
import todoapp.view.todo.Edit;

import java.util.*;

import static spark.Spark.*;

public class App {

  public static void main(String[] args) {

    exception(Exception.class, (e, req, res) -> e.printStackTrace()); // print all exceptions
    staticFiles.location("/public");
    port(9999);

    // Render main UI
    get("/", (req, res) -> renderTodos(req));

    // Add new
    post("/todos", (req, res) -> {
      ToDo.DAO.add(ToDo.create(req.queryParams("todo-title")));
      return renderTodos(req);
    });

    // Remove all completed
    delete("/todos/completed", (req, res) -> {
      ToDo.DAO.removeCompleted();
      return renderTodos(req);
    });

    // Toggle all status
    put("/todos/toggle_status", (req, res) -> {
      ToDo.DAO.toggleAll(req.queryParams("toggle-all") != null);
      return renderTodos(req);
    });

    // Remove by id
    delete("/todos/:id", (req, res) -> {
      ToDo.DAO.remove(req.params("id"));
      return renderTodos(req);
    });

    // Update by id
    put("/todos/:id", (req, res) -> {
      ToDo.DAO.update(req.params("id"), req.queryParams("todo-title"));
      return renderTodos(req);
    });

    // Toggle status by id
    put("/todos/:id/toggle_status", (req, res) -> {
      ToDo.DAO.toggleStatus(req.params("id"));
      return renderTodos(req);
    });

    // Edit by id
    get("/todos/:id/edit", (req, res) -> renderEditTodo(req));
  }

  private static String renderEditTodo(Request req) {
    return Edit.render(ILayout.EMPTY, ToDo.DAO.find(req.params("id")));
  }

  private static String renderTodos(Request req) {

    String statusStr = req.queryParams("status");

    List<ToDo> todos = ToDo.DAO.ofStatus(statusStr);
    String filter = Optional.ofNullable(statusStr).orElse("");
    int activeCount = ToDo.DAO.ofStatus(Status.ACTIVE).size();
    boolean anyComplete = ToDo.DAO.ofStatus(Status.COMPLETE).size() > 0;
    boolean allComplete = ToDo.DAO.all().size() == ToDo.DAO.ofStatus(Status.COMPLETE).size();

    if ("true".equals(req.queryParams("ic-request"))) {
      return Display.render(todos, filter, activeCount, anyComplete, allComplete);
    } else {
      return Display.render(Main.asLayout(), todos, filter, activeCount, anyComplete, allComplete);
    }
  }


}