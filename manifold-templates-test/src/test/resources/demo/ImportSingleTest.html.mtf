<!DOCTYPE html>
<%@ import java.util.TreeSet %>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Import Single Test</title>
</head>
<body>
    <h1>I am going to import some stuff right now</h1>
    <p>About to use the TreeMap </p>
    <% TreeSet<Integer> myTreeSet = new TreeSet<>(); %>
    <% for (int i = 0; i < 10; i += 1) {
        myTreeSet.add(i);
    }
    for(int a: myTreeSet) { %>
        <p> This is paragraph ${a} </p>
       <% }%>
</body>
</html>