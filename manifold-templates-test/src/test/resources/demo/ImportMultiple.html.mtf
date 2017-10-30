<!DOCTYPE html>
<%@ import java.util.LinkedList %>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Test for Multiple Imports</title>
</head>
<body>
    <h1>This test will make sure that multiple imports in random places is valid</h1>
    <% LinkedList<Integer> myLinkedList = new LinkedList<>();
       HashSet<LinkedList<Integer>> myHashSet = new HashSet<>(); %>
    <%@ import java.util.HashSet %>
    <% TreeSet<Integer> myTreeSet = new TreeSet<>();
       myLinkedList.add(5);
       myHashSet.add(myLinkedList);
       myTreeSet.add(100);
       for(Integer a: myTreeSet) { %>
            <h1> ${a} </h1>
        <% } %>

</body>
</html>
<%@ import java.util.TreeSet %>